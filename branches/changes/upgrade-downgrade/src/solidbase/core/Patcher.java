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
	static protected Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE[ \\t]+SQL[ \\t]+ERROR[ \\t]+(\\w+([ \\t]*,[ \\t]*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static protected Pattern ignoreEnd = Pattern.compile( "/IGNORE[ \\t]+SQL[ \\t]+ERROR", Pattern.CASE_INSENSITIVE );

	// TODO Do we need the whitespace at the end?
	static protected Pattern setUserPattern = Pattern.compile( "SET[ \\t]+USER[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );
	static protected Pattern selectConnectionPattern = Pattern.compile( "SELECT[ \\t]+CONNECTION[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );

	static protected Pattern startMessagePattern = Pattern.compile( "\\s*(?:SET\\s+MESSAGE|MESSAGE\\s+START)\\s+['\"]([^'\"]*)['\"]\\s*", Pattern.CASE_INSENSITIVE );

	static protected Pattern sessionConfigPattern = Pattern.compile( "SESSIONCONFIG", Pattern.CASE_INSENSITIVE );
	static protected Pattern sessionConfigPatternEnd = Pattern.compile( "/SESSIONCONFIG", Pattern.CASE_INSENSITIVE );

	static protected Pattern ifHistoryContainsPattern = Pattern.compile( "IF\\s+HISTORY\\s+(NOT\\s+)?CONTAINS\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE );
	static protected Pattern ifHistoryContainsEnd = Pattern.compile( "/IF", Pattern.CASE_INSENSITIVE );

	static protected List< CommandListener > listeners = new ArrayList();

	// Patch state
	static protected Stack ignoreStack = new Stack();
	static protected HashSet ignoreSet = new HashSet();
	static protected boolean dontCount;
	static protected Stack<Boolean> conditionStack = new Stack();
	static protected boolean condition = true;

	static protected ProgressListener callBack;
	static protected PatchFile patchFile;
	static protected Database defaultDatabase;
	static protected Database database;
	static protected Map< String, Database > allDatabases = new HashMap< String, Database >();
	static protected DBVersion dbVersion;

	static
	{
		listeners.add( new AssertCommandExecuter() );
		//listeners.add( new OracleDBMSOutputPoller() );
		listeners.add( new ImportCSVListener() );
	}

	static public void openPatchFile( String fileName )
	{
		openPatchFile( null, fileName );
	}

	static public void openPatchFile( File baseDir, String fileName )
	{
		if( fileName == null )
			fileName = "dbpatch.sql";

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
			catch( IOException e )
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

	static public void closePatchFile()
	{
		if( patchFile != null )
		{
			try
			{
				patchFile.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
		patchFile = null;
	}

	static public String getCurrentVersion()
	{
		return dbVersion.getVersion();
	}

	static public String getCurrentTarget()
	{
		return dbVersion.getTarget();
	}

	static public int getCurrentStatements()
	{
		return dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets.
	 * 
	 * @param tips If true only the tips of the patch paths are returned.
	 * @param prefix Only consider versions that start with the given prefix.
	 * @return
	 */
	static public LinkedHashSet< String > getTargets( boolean tips, String prefix )
	{
		LinkedHashSet result = new LinkedHashSet();
		patchFile.collectTargets( dbVersion.getVersion(), dbVersion.getTarget(), tips, false, prefix, result );
		return result;
	}

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

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target
	 * @throws SQLException
	 */
	static public void patch( String target ) throws SQLExecutionException
	{
		init();

		Set< String > targets;

		boolean wildcard = target.endsWith( "*" );
		if( wildcard )
		{
			String targetPrefix = target.substring( 0, target.length() - 1 );
			targets = getTargets( true, targetPrefix );
			for( String t : targets )
				if( t.startsWith( targetPrefix ) )
				{
					patch( dbVersion.getVersion(), t );
					break;
				}
		}
		else
		{
			targets = getTargets( false, null );
			for( String t : targets )
				if( t.equals( target ) )
				{
					patch( dbVersion.getVersion(), t );
					break;
				}
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

	static protected void patch( String version, String target ) throws SQLExecutionException
	{
		if( target.equals( version ) )
			return;

		List patches = patchFile.getPatchPath( version, target );
		Assert.isTrue( patches != null );
		Assert.isTrue( patches.size() > 0, "No patches found" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}

	static protected SQLException execute( Command command ) throws SQLException
	{
		Assert.isTrue( command.isNonRepeatable() );

		String sql = command.getCommand();
		if( sql.length() > 0 )
		{
			for( Iterator iter = listeners.iterator(); iter.hasNext(); )
			{
				CommandListener listener = (CommandListener)iter.next();
				if( listener.execute( database, command ) )
					return null;
			}

			try
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
			catch( SQLException e )
			{
				String error = e.getSQLState();
				if( ignoreSet.contains( error ) )
					return e;
				Patcher.callBack.exception( command );
				throw e;
			}
		}

		return null;
	}

	static protected void patch( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch, "patch == null" );

		Patcher.callBack.patchStarting( patch.getSource(), patch.getTarget() );

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
		conditionStack.clear();
		condition = true;

		Command command = patchFile.readStatement();
		int count = 0;
		try
		{
			while( command != null )
			{
				String sql = command.getCommand();

				if( command.isRepeatable() ) // TODO Rename to isDBPatcherCommand
				{
					boolean done = false;
					for( Iterator iter = listeners.iterator(); iter.hasNext(); )
					{
						CommandListener listener = (CommandListener)iter.next();
						done = listener.execute( database, command );
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

					execute( command );
					Patcher.callBack.executed();
				}
				else
				{
					count++;
					if( count > skip && condition )
					{
						Patcher.callBack.executing( command, startMessage );
						startMessage = null;

						if( sql.trim().equalsIgnoreCase( "UPGRADE" ) )
							upgrade( patch );
						else
						{
							SQLException sqlException = execute( command );
							if( !patch.isInit() )
							{
								dbVersion.setProgress( patch.getTarget(), count );
								// We have to update the progress even if the logging fails. Otherwise the patch cannot be
								// restarted. That's why the progress update is first. But some logging will be lost in that case.
								if( sqlException != null )
									dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), sqlException );
								else
									dbVersion.log( "S", patch.getSource(), patch.getTarget(), count, sql, (String)null );
							}
						}
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
			else if( !patch.isOpen() )
			{
				dbVersion.setVersion( patch.getTarget() );
				dbVersion.logComplete( patch.getSource(), patch.getTarget(), count );
			}
		}
		catch( RuntimeException e )
		{
			dbVersion.log( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw e;
		}
		catch( SQLException e )
		{
			dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw new SQLExecutionException( command, e );
		}
	}

	static private void upgrade( Patch patch ) throws SQLException
	{
		Assert.isFalse( !patch.isInit(), "UPGRADE only allowed in INIT blocks" );
		Assert.isTrue( patch.getSource().equals( "1.0" ) && patch.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		execute( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false ) );
		execute( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETED' WHERE RESULT LIKE 'COMPLETED VERSION %'", false ) );
		execute( new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false ) ); // We need this because the column is made NOT NULL in the upgrade init block
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
		if( condition )
		{
			boolean c = dbVersion.logContains( version );
			if( not != null )
				c = !c;
			conditionStack.push( condition );
			condition = c;
		}
		else
			conditionStack.push( false );
	}

	private static void ifHistoryContainsEnd()
	{
		condition = conditionStack.pop();
	}

	static public ProgressListener getCallBack()
	{
		return Patcher.callBack;
	}

	static public void setCallBack( ProgressListener callBack )
	{
		Patcher.callBack = callBack;
	}

	static public void logToXML( String filename )
	{
		if( filename.equals( "-" ) )
			dbVersion.logToXML( System.out, Charset.defaultCharset() );
		else
		{
			try
			{
				dbVersion.logToXML( new FileOutputStream( filename ), Charset.forName( "UTF-8" ) );
			}
			catch( FileNotFoundException e )
			{
				throw new SystemException( e );
			}
		}
	}

	static public void end()
	{
		closePatchFile();
		if( database != null )
			database.closeConnections();

		// TODO More like these?
		defaultDatabase = null;
		database = null;
		allDatabases = new HashMap< String, Database >();
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
