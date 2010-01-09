/*--
 * Copyright 2006 Ren� M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Patcher
{
	static private Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE[ \\t]+SQL[ \\t]+ERROR[ \\t]+(\\w+([ \\t]*,[ \\t]*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static private Pattern ignoreEnd = Pattern.compile( "/IGNORE[ \\t]+SQL[ \\t]+ERROR", Pattern.CASE_INSENSITIVE );

	// TODO Do we need the whitespace at the end?
	static private Pattern setUserPattern = Pattern.compile( "SET[ \\t]+USER[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );
	static private Pattern selectConnectionPattern = Pattern.compile( "SELECT[ \\t]+CONNECTION[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );

	static private Pattern startMessagePattern = Pattern.compile( "\\s*(?:SET\\s+MESSAGE|MESSAGE\\s+START)\\s+['\"]([^'\"]*)['\"]\\s*", Pattern.CASE_INSENSITIVE );

	static private Pattern sessionConfigPattern = Pattern.compile( "SESSIONCONFIG", Pattern.CASE_INSENSITIVE );
	static private Pattern sessionConfigPatternEnd = Pattern.compile( "/SESSIONCONFIG", Pattern.CASE_INSENSITIVE );

	static private Pattern ifHistoryContainsPattern = Pattern.compile( "IF\\s+HISTORY\\s+(NOT\\s+)?CONTAINS\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE );
	static private Pattern ifHistoryContainsEnd = Pattern.compile( "/IF", Pattern.CASE_INSENSITIVE );

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	static protected List< CommandListener > listeners;

	// Patch state

	/**
	 * Errors that should be ignored.
	 */
	static protected Stack ignoreStack;

	/**
	 * Errors that should be ignored. This set is kept in sync with the {@link Patcher#ignoreStack}.
	 */
	static protected HashSet ignoreSet;

	/**
	 * Indicates that the statements are transient and should not be counted.
	 */
	static protected boolean dontCount;

	/**
	 * Together with {@link Patcher#conditionFalseCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link Patcher#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link Patcher#conditionFalseCounter} get incremented.
	 */
	static protected int conditionTrueCounter;

	/**
	 * Together with {@link Patcher#conditionTrueCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link Patcher#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link Patcher#conditionFalseCounter} get incremented.
	 */
	static protected int conditionFalseCounter;

	/**
	 * The progress listener.
	 */
	static protected ProgressListener callBack;

	/**
	 * The upgrade file being executed.
	 */
	static protected PatchFile patchFile;

	/**
	 * The default database. At the start of each change set, this database is put into {@link Patcher#database} to become the current database.
	 */
	static protected Database defaultDatabase;

	/**
	 * The current database. Gets reset to the default database at the start of each change set.
	 */
	static protected Database database;

	/**
	 * All configured databases. This is used when the upgrade file selects a different database by name.
	 */
	static protected Map< String, Database > allDatabases;

	/**
	 * The class that manages the DBVERSION and DBVERSIONLOG table.
	 */
	static protected DBVersion dbVersion;

	static
	{
		initialize();
	}

	/**
	 * Initialize the patcher.
	 */
	static protected void initialize()
	{
		defaultDatabase = null;
		database = null;
		allDatabases = new HashMap< String, Database >();

		listeners = new ArrayList();

		ignoreStack = new Stack();
		ignoreSet = new HashSet();
		dontCount = false;
		conditionTrueCounter = conditionFalseCounter = 0;

		callBack = null;
		patchFile = null;
		dbVersion = null;

		listeners.add( new AssertCommandExecuter() );
		listeners.add( new ImportCSVListener() );
	}

	/**
	 * Open the specified upgrade file.
	 * 
	 * @param fileName The name and path of the upgrade file.
	 */
	static public void openPatchFile( String fileName )
	{
		openPatchFile( null, fileName );
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the upgrade file.
	 */
	static public void openPatchFile( File baseDir, String fileName )
	{
		if( fileName == null )
			fileName = "upgrade.sql";

		try
		{
			RandomAccessLineReader ralr;
			// TODO Should we remove this "/"?
			URL url = Patcher.class.getResource( "/" + fileName ); // In the classpath
			if( url != null )
			{
				callBack.openingPatchFile( url );
				ralr = new RandomAccessLineReader( url );
			}
			else
			{
				File file = new File( baseDir, fileName ); // In the current folder
				callBack.openingPatchFile( file );
				ralr = new RandomAccessLineReader( file );
			}

			patchFile = new PatchFile( ralr );

			callBack.openedPatchFile( patchFile );

			// Need to close in case of an exception during reading
			try
			{
				patchFile.read();
			}
			catch( RuntimeException e )
			{
				patchFile.close();
				throw e;
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Close the upgrade file.
	 */
	static public void closePatchFile()
	{
		if( patchFile != null )
			patchFile.close();
		patchFile = null;
	}

	/**
	 * Get the current version of the database.
	 * 
	 * @return The current version of the database.
	 */
	static public String getCurrentVersion()
	{
		return dbVersion.getVersion();
	}

	/**
	 * Get the current target.
	 * 
	 * @return The current target.
	 */
	static public String getCurrentTarget()
	{
		return dbVersion.getTarget();
	}

	/**
	 * Get the number of persistent statements executed successfully.
	 * 
	 * @return The number of persistent statements executed successfully.
	 */
	static public int getCurrentStatements()
	{
		return dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets from the current version.
	 * 
	 * @param tips If true only the tips of the upgrade paths are returned.
	 * @param prefix Only return targets that start with the given prefix.
	 * @param downgradeable Also consider downgrade paths.
	 * @return All possible targets from the current version.
	 */
	static public LinkedHashSet< String > getTargets( boolean tips, String prefix, boolean downgradeable )
	{
		LinkedHashSet result = new LinkedHashSet();
		patchFile.collectTargets( dbVersion.getVersion(), dbVersion.getTarget(), tips, downgradeable, prefix, result );
		return result;
	}

	/**
	 * Use the 'init' change sets to upgrade the DBVERSION and DBVERSIONLOG tables to the newest specification.
	 * 
	 * @throws SQLExecutionException Thrown when the execution of an SQL statement fails.
	 */
	static public void init() throws SQLExecutionException
	{
		String spec = dbVersion.getSpec();

		List patches = patchFile.getInitPath( spec );
		if( patches == null )
			return;

		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		// INIT blocks get special treatment.
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
			dbVersion.setSpec( patch.getTarget() );
			// TODO How do we get a more dramatic error message here, if something goes wrong?
		}
	}

	static public void patch( String target ) throws SQLExecutionException
	{
		patch( target, false );
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target
	 * @throws SQLException
	 */
	static public void patch( String target, boolean downgradeable ) throws SQLExecutionException
	{
		init();

		Set< String > targets;

		boolean wildcard = target.endsWith( "*" );
		if( wildcard )
		{
			String targetPrefix = target.substring( 0, target.length() - 1 );
			targets = getTargets( true, targetPrefix, downgradeable );
			Assert.isTrue( targets.size() <= 1 );
			for( String t : targets )
				if( t.startsWith( targetPrefix ) )
				{
					patch( dbVersion.getVersion(), t, downgradeable );
					break;
				}
			// TODO What if the target is not found?
		}
		else
		{
			targets = getTargets( false, null, downgradeable );
			for( String t : targets )
				if( t.equals( target ) )
				{
					patch( dbVersion.getVersion(), t, downgradeable );
					break;
				}
			// TODO What if the target is not found?
		}

		terminateCommandListeners();
		if( targets.size() > 0 )
			callBack.patchingFinished();
		else
			throw new SystemException( "Target " + target + " is not a possible target" );
	}

	static protected void terminateCommandListeners()
	{
		for( CommandListener listener : listeners )
			listener.terminate();
	}

	/**
	 * Configures the connection to the database, including the default user. Each patch in the patch file starts with
	 * this database and default user. The version tables are also looked for in this database and the schema identified
	 * by the default user.
	 * 
	 * @param database The default database.
	 */
	static public void setDefaultConnection( Database database )
	{
		Patcher.defaultDatabase = database;
		Patcher.allDatabases.put( "default", database );

		database.init(); // Resets the current user and initializes the connection when password is supplied.

		dbVersion = new DBVersion( database );

		callBack.debug( "driverName=" + database.driverName + ", url=" + database.url + ", user=" + database.getDefaultUser() + "" );
	}

	static protected void patch( String version, String target, boolean downgradeable ) throws SQLExecutionException
	{
		if( target.equals( version ) )
			return;

		List patches = patchFile.getPatchPath( version, target, downgradeable );
		Assert.isTrue( patches != null );
		Assert.isTrue( patches.size() > 0, "No upgrades found" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}

	static protected boolean executeListeners( Command command ) throws SQLException
	{
		for( Iterator iter = listeners.iterator(); iter.hasNext(); )
		{
			CommandListener listener = (CommandListener)iter.next();
			if( listener.execute( database, command ) )
				return true;
		}
		return false;
	}

	// count == 0 --> non counting
	static protected void execute( Patch patch, Command command, int count ) throws SQLExecutionException
	{
		Assert.isTrue( command.isPersistent() );

		String sql = command.getCommand();
		if( sql.length() > 0 )
		{
			try
			{
				if( !executeListeners( command ) )
				{
					Connection connection = database.getConnection();
					Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
					Statement statement = connection.createStatement();
					boolean commit = false;
					try
					{
						statement.execute( sql );
						commit = true;
					}
					finally
					{
						statement.close();
						if( commit )
							connection.commit();
						else
							connection.rollback();
					}
				}
			}
			catch( SQLException e )
			{
				if( patch.isInit() )
				{
					Patcher.callBack.exception( command );
					throw new SQLExecutionException( command, e );
				}
				dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
				String error = e.getSQLState();
				if( !ignoreSet.contains( error ) )
				{
					Patcher.callBack.exception( command );
					throw new SQLExecutionException( command, e );
				}
			}

			if( !patch.isInit() )
			{
				if( count > 0 )
				{
					// We have to update the progress even if the logging fails. Otherwise the patch cannot be
					// restarted. That's why the progress update is first. But some logging will be lost in that case.
					dbVersion.setProgress( patch.getTarget(), count );
					dbVersion.log( "S", patch.getSource(), patch.getTarget(), count, sql, (String)null );
				}
				return;
			}
		}
	}

	static protected void patch( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch, "patch == null" );

		Patcher.callBack.patchStarting( patch );

		patchFile.gotoPatch( patch );
		int skip = dbVersion.getStatements();
		if( dbVersion.getTarget() == null )
			skip = 0;

		String startMessage = null;

		// Reset previous patch state
		setConnection( defaultDatabase ); // Also resets the current user for the connection
		ignoreStack.clear();
		ignoreSet.clear();
		dontCount = false;
		conditionFalseCounter = conditionTrueCounter = 0;

		Command command = patchFile.readStatement();
		int count = 0;
		while( command != null )
		{
			String sql = command.getCommand();

			if( command.isTransient() )
			{
				boolean done = false;
				for( Iterator iter = listeners.iterator(); iter.hasNext(); )
				{
					CommandListener listener = (CommandListener)iter.next();
					try
					{
						done = listener.execute( database, command );
					}
					catch( SQLException e )
					{
						// TODO The listener should use Patcher.execute() so that we don't need the catch here.
						if( !patch.isInit() )
						{
							dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
							String error = e.getSQLState();
							if( ignoreSet.contains( error ) )
								return;
						}
						Patcher.callBack.exception( command );
						throw new SQLExecutionException( command, e );
					}
					if( done )
						break;
				}

				if( !done )
				{
					Matcher matcher;
					if( ( matcher = ignoreSqlErrorPattern.matcher( sql ) ).matches() )
						pushIgnores( matcher.group( 1 ) );
					else if( ignoreEnd.matcher( sql ).matches() )
						popIgnores();
					else if( ( matcher = setUserPattern.matcher( sql ) ).matches() )
						setUser( matcher.group( 1 ) );
					else if( ( matcher = startMessagePattern.matcher( sql ) ).matches() )
						startMessage = matcher.group( 1 );
					else if( sessionConfigPattern.matcher( sql ).matches() )
						enableDontCount();
					else if( sessionConfigPatternEnd.matcher( sql ).matches() )
						disableDontCount();
					else if( ( matcher = ifHistoryContainsPattern.matcher( sql ) ).matches() )
						ifHistoryContains( matcher.group( 1 ), matcher.group( 2 ) );
					else if( ifHistoryContainsEnd.matcher( sql ).matches() )
						ifHistoryContainsEnd();
					else if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
						selectConnection( matcher.group( 1 ) );
					else
						Assert.fail( "Unknown command [" + sql + "]" );
				}
			}
			else if( dontCount )
			{
				Patcher.callBack.executing( command, startMessage );
				startMessage = null;
				execute( patch, command, 0 );
				Patcher.callBack.executed();
			}
			else
			{
				count++;
				if( count > skip && conditionFalseCounter == 0 )
				{
					Patcher.callBack.executing( command, startMessage );
					startMessage = null;
					if( sql.trim().equalsIgnoreCase( "UPGRADE" ) )
						upgrade( patch, command );
					else
						execute( patch, command, count );
					Patcher.callBack.executed();
				}
				else
					Patcher.callBack.skipped( command );
			}

			command = patchFile.readStatement();
		}
		Patcher.callBack.patchFinished();

		dbVersion.setStale(); // TODO With a normal patch, only set stale if not both of the 2 version tables are found
		if( patch.isInit() )
		{
			dbVersion.setSpec( patch.getTarget() );
			Assert.isFalse( patch.isOpen() );
		}
		else
		{
			if( patch.isDowngrade() )
			{
				Set versions = patchFile.getReachableVersions( patch.getTarget(), null, false );
				versions.remove( patch.getTarget() );
				dbVersion.downgradeHistory( versions );
			}
			if( !patch.isOpen() )
			{
				dbVersion.setVersion( patch.getTarget() );
				dbVersion.logComplete( patch.getSource(), patch.getTarget(), count );
			}
		}
	}

	static private void upgrade( Patch patch, Command command ) throws SQLExecutionException
	{
		Assert.isFalse( !patch.isInit(), "UPGRADE only allowed in INIT blocks" );
		Assert.isTrue( patch.getSource().equals( "1.0" ) && patch.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		int pos = command.getLineNumber();
		execute( patch, new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false, pos ), 0 );
		execute( patch, new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETE' WHERE RESULT LIKE 'COMPLETED VERSION %'", false, pos ), 0 );
		execute( patch, new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false, pos ), 0 ); // We need this because the column is made NOT NULL in the upgrade init block
	}

	static private void setConnection( Database database )
	{
		Patcher.database = database;
		database.init(); // Reset the current user
	}

	static protected void setUser( String user )
	{
		database.setCurrentUser( user );
	}

	static protected void pushIgnores( String ignores )
	{
		String[] ss = ignores.split( "," );
		for( int i = 0; i < ss.length; i++ )
			ss[ i ] = ss[ i ].trim();
		ignoreStack.push( ss );
		refreshIgnores();
	}

	static protected void popIgnores()
	{
		ignoreStack.pop();
		refreshIgnores();
	}

	static protected void refreshIgnores()
	{
		HashSet ignores = new HashSet();
		for( Iterator iter = ignoreStack.iterator(); iter.hasNext(); )
		{
			String[] ss = (String[])iter.next();
			for( int i = 0; i < ss.length; i++ )
				ignores.add( ss[ i ] );
		}
		ignoreSet = ignores;
	}

	static protected void enableDontCount()
	{
		Assert.isFalse( dontCount, "Counting already enabled" );
		dontCount = true;
	}

	static protected void disableDontCount()
	{
		Assert.isTrue( dontCount, "Counting already disabled" );
		dontCount = false;
	}

	private static void ifHistoryContains( String not, String version )
	{
		if( conditionFalseCounter == 0 )
		{
			boolean c = dbVersion.logContains( version );
			if( not != null )
				c = !c;
			if( c )
				conditionTrueCounter++;
			else
				conditionFalseCounter++;
		}
		else
			conditionFalseCounter++;
	}

	private static void ifHistoryContainsEnd()
	{
		if( conditionFalseCounter > 0 )
			conditionFalseCounter--;
		else
		{
			Assert.isTrue( conditionTrueCounter > 0 );
			conditionTrueCounter--;
		}
	}

	static public ProgressListener getCallBack()
	{
		return Patcher.callBack;
	}

	static public void setCallBack( ProgressListener callBack )
	{
		Patcher.callBack = callBack;
	}

	static public void logToXML( OutputStream out )
	{
		dbVersion.logToXML( out, Charset.forName( "UTF-8" ) );
	}

	static public void logToXML( String filename )
	{
		if( filename.equals( "-" ) )
			logToXML( System.out );
		else
		{
			try
			{
				logToXML( new FileOutputStream( filename ) );
			}
			catch( FileNotFoundException e )
			{
				throw new SystemException( e );
			}
		}
	}

	// TODO This is caused by being a static class
	static public void end()
	{
		closePatchFile();
		if( database != null )
			database.closeConnections();

		initialize();
	}

	static protected void selectConnection( String name )
	{
		name = name.toLowerCase();
		Database database = allDatabases.get( name );
		Assert.notNull( database, "Database '" + name + "' (case-insensitive) not known" );
		setConnection( database );
	}

	static public void addConnection( solidbase.config.Connection connection )
	{
		Assert.notNull( defaultDatabase );
		String driver = connection.getDriver();
		String url = connection.getUrl();
		allDatabases.put( connection.getName(), new Database( driver != null ? driver : defaultDatabase.driverName, url != null ? url : defaultDatabase.url, connection.getUser().toLowerCase(), connection.getPassword() ) );
	}

	static public void connect()
	{
		defaultDatabase.getConnection();
	}
}