/*--
 * Copyright 2006 René M. de Bloois
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
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Patcher
{
	// Don't need whitespace at the end

	static private Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE\\s+SQL\\s+ERROR\\s+(\\w+(\\s*,\\s*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static private Pattern ignoreEnd = Pattern.compile( "/IGNORE\\s+SQL\\s+ERROR", Pattern.CASE_INSENSITIVE );

	static private Pattern setUserPattern = Pattern.compile( "SET\\s+USER\\s+(\\w+)\\s*", Pattern.CASE_INSENSITIVE );
	static private Pattern selectConnectionPattern = Pattern.compile( "SELECT\\s+CONNECTION\\s+(\\w+)", Pattern.CASE_INSENSITIVE );

	static private Pattern startMessagePattern = Pattern.compile( "(?:SET\\s+MESSAGE|MESSAGE\\s+START)\\s+[\"](.*)[\"]", Pattern.CASE_INSENSITIVE );

	static private Pattern sessionConfigPattern = Pattern.compile( "SESSIONCONFIG", Pattern.CASE_INSENSITIVE );
	static private Pattern sessionConfigPatternEnd = Pattern.compile( "/SESSIONCONFIG", Pattern.CASE_INSENSITIVE );

	static private Pattern ifHistoryContainsPattern = Pattern.compile( "IF\\s+HISTORY\\s+(NOT\\s+)?CONTAINS\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE );
	static private Pattern ifHistoryContainsEnd = Pattern.compile( "/IF", Pattern.CASE_INSENSITIVE );

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	protected List< CommandListener > listeners;

	// Context

	/**
	 * The message that should be shown when a statement is executed.
	 */
	protected String startMessage;

	/**
	 * Errors that should be ignored.
	 */
	protected Stack ignoreStack;

	/**
	 * Errors that should be ignored. This set is kept in sync with the {@link #ignoreStack}.
	 */
	protected HashSet ignoreSet;

	/**
	 * Indicates that the statements are transient and should not be counted.
	 */
	protected boolean dontCount;

	/**
	 * Together with {@link Patcher#conditionFalseCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link Patcher#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link Patcher#conditionFalseCounter} get incremented.
	 */
	protected int conditionTrueCounter;

	/**
	 * Together with {@link Patcher#conditionTrueCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link Patcher#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link Patcher#conditionFalseCounter} get incremented.
	 */
	protected int conditionFalseCounter;

	/**
	 * The progress listener.
	 */
	protected ProgressListener callBack;

	/**
	 * The upgrade file being executed.
	 */
	protected PatchFile patchFile;

	/**
	 * The default database. At the start of each change set, this database is put into {@link Patcher#currentDatabase} to become the current database.
	 */
	protected Database defaultDatabase;

	/**
	 * The current database. Gets reset to the default database at the start of each change set.
	 */
	protected Database currentDatabase;

	/**
	 * All configured databases. This is used when the upgrade file selects a different database by name.
	 */
	protected Map< String, Database > databases;

	/**
	 * The class that manages the DBVERSION and DBVERSIONLOG table.
	 */
	protected DBVersion dbVersion;

	/**
	 * Construct a new instance of the patcher.
	 * 
	 * @param listener
	 * @param database The default database to use. This database also contains a default user. Version tables will be looked for in this database in the schema identified by the default user.
	 */
	public Patcher( ProgressListener listener, Database database )
	{
		this.callBack = listener;

		this.defaultDatabase = database;
		this.databases = new HashMap< String, Database >();
		this.databases.put( "default", database );
		this.currentDatabase = null;

		database.init(); // Resets the current user and initializes the connection when password is supplied.

		this.dbVersion = new DBVersion( database, this.callBack );

		this.listeners = new ArrayList();
		this.listeners.add( new AssertCommandExecuter() );
		this.listeners.add( new ImportCSVListener() );

		reset();

		this.patchFile = null; // TODO Should be argument to the constructor

		this.callBack.debug( "driverName=" + database.driverName + ", url=" + database.url + ", user=" + database.getDefaultUser() + "" );
	}

	/**
	 * Resets the upgrade context. This is called before the execution of each changeset.
	 */
	protected void reset()
	{
		setConnection( this.defaultDatabase ); // Also resets the current user for the connection
		this.startMessage = null;
		this.ignoreStack = new Stack();
		this.ignoreSet = new HashSet();
		this.dontCount = false;
		this.conditionTrueCounter = this.conditionFalseCounter = 0;
	}

	/**
	 * Open the specified upgrade file.
	 * 
	 * @param fileName The name and path of the upgrade file.
	 */
	public void openPatchFile( String fileName )
	{
		openPatchFile( null, fileName );
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the upgrade file.
	 */
	public void openPatchFile( File baseDir, String fileName )
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
				this.callBack.openingPatchFile( url );
				ralr = new RandomAccessLineReader( url );
			}
			else
			{
				File file = new File( baseDir, fileName ); // In the current folder
				this.callBack.openingPatchFile( file );
				ralr = new RandomAccessLineReader( file );
			}

			this.patchFile = new PatchFile( ralr );

			this.callBack.openedPatchFile( this.patchFile );

			// Need to close in case of an exception during reading
			try
			{
				this.patchFile.read();
			}
			catch( RuntimeException e )
			{
				this.patchFile.close();
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
	public void closePatchFile()
	{
		if( this.patchFile != null )
			this.patchFile.close();
		this.patchFile = null;
	}

	/**
	 * Get the current version of the database.
	 * 
	 * @return The current version of the database.
	 */
	public String getCurrentVersion()
	{
		return this.dbVersion.getVersion();
	}

	/**
	 * Get the current target.
	 * 
	 * @return The current target.
	 */
	public String getCurrentTarget()
	{
		return this.dbVersion.getTarget();
	}

	/**
	 * Get the number of persistent statements executed successfully.
	 * 
	 * @return The number of persistent statements executed successfully.
	 */
	public int getCurrentStatements()
	{
		return this.dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets from the current version.
	 * 
	 * @param tips If true only the tips of the upgrade paths are returned.
	 * @param prefix Only return targets that start with the given prefix.
	 * @param downgradeable Also consider downgrade paths.
	 * @return All possible targets from the current version.
	 */
	public LinkedHashSet< String > getTargets( boolean tips, String prefix, boolean downgradeable )
	{
		LinkedHashSet result = new LinkedHashSet();
		this.patchFile.collectTargets( this.dbVersion.getVersion(), this.dbVersion.getTarget(), tips, downgradeable, prefix, result );
		return result;
	}

	/**
	 * Use the 'init' change sets to upgrade the DBVERSION and DBVERSIONLOG tables to the newest specification.
	 * 
	 * @throws SQLExecutionException Thrown when the execution of an SQL statement fails.
	 */
	public void init() throws SQLExecutionException
	{
		String spec = this.dbVersion.getSpec();

		List patches = this.patchFile.getInitPath( spec );
		if( patches == null )
			return;

		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		// INIT blocks get special treatment.
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
			this.dbVersion.updateSpec( patch.getTarget() );
			// TODO How do we get a more dramatic error message here, if something goes wrong?
		}
	}

	public void patch( String target ) throws SQLExecutionException
	{
		patch( target, false );
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target
	 * @throws SQLException
	 */
	public void patch( String target, boolean downgradeable ) throws SQLExecutionException
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
					patch( this.dbVersion.getVersion(), t, downgradeable );
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
					patch( this.dbVersion.getVersion(), t, downgradeable );
					break;
				}
			// TODO What if the target is not found?
		}

		terminateCommandListeners();
		if( targets.size() > 0 )
			this.callBack.patchingFinished();
		else
			throw new SystemException( "Target " + target + " is not a possible target" );
	}

	protected void terminateCommandListeners()
	{
		for( CommandListener listener : this.listeners )
			listener.terminate();
	}

	protected void patch( String version, String target, boolean downgradeable ) throws SQLExecutionException
	{
		if( target.equals( version ) )
			return;

		List patches = this.patchFile.getPatchPath( version, target, downgradeable );
		Assert.isTrue( patches != null );
		Assert.isTrue( patches.size() > 0, "No upgrades found" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}

	protected boolean executeListeners( Command command ) throws SQLException
	{
		for( Iterator iter = this.listeners.iterator(); iter.hasNext(); )
		{
			CommandListener listener = (CommandListener)iter.next();
			if( listener.execute( this.currentDatabase, command ) )
				return true;
		}
		return false;
	}

	// count == 0 --> non counting
	protected void execute( Patch patch, Command command, int count ) throws SQLExecutionException
	{
		Assert.isTrue( command.isPersistent() );

		String sql = command.getCommand();
		if( sql.length() > 0 )
		{
			try
			{
				if( !executeListeners( command ) )
				{
					Connection connection = this.currentDatabase.getConnection();
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
					this.callBack.exception( command );
					throw new SQLExecutionException( command, e );
				}
				this.dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
				String error = e.getSQLState();
				if( !this.ignoreSet.contains( error ) )
				{
					this.callBack.exception( command );
					throw new SQLExecutionException( command, e );
				}
			}

			if( !patch.isInit() )
			{
				if( count > 0 )
				{
					// We have to update the progress even if the logging fails. Otherwise the patch cannot be
					// restarted. That's why the progress update is first. But some logging will be lost in that case.
					this.dbVersion.updateProgress( patch.getTarget(), count );
					this.dbVersion.log( "S", patch.getSource(), patch.getTarget(), count, sql, (String)null );
				}
				return;
			}
		}
	}

	protected void patch( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch, "patch == null" );

		this.callBack.patchStarting( patch );

		this.patchFile.gotoPatch( patch );
		int skip = this.dbVersion.getStatements();
		if( this.dbVersion.getTarget() == null )
			skip = 0;

		reset();

		Command command = this.patchFile.readStatement();
		int count = 0;
		while( command != null )
		{
			String sql = command.getCommand();

			if( command.isTransient() )
			{
				boolean done = false;
				for( Iterator iter = this.listeners.iterator(); iter.hasNext(); )
				{
					CommandListener listener = (CommandListener)iter.next();
					try
					{
						done = listener.execute( this.currentDatabase, command );
					}
					catch( SQLException e )
					{
						// TODO The listener should use Patcher.execute() so that we don't need the catch here.
						if( !patch.isInit() )
						{
							this.dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
							String error = e.getSQLState();
							if( this.ignoreSet.contains( error ) )
								return;
						}
						this.callBack.exception( command );
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
						this.startMessage = matcher.group( 1 );
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
			else if( this.dontCount )
			{
				this.callBack.executing( command, this.startMessage );
				this.startMessage = null;
				execute( patch, command, 0 );
				this.callBack.executed();
			}
			else
			{
				count++;
				if( count > skip && this.conditionFalseCounter == 0 )
				{
					this.callBack.executing( command, this.startMessage );
					this.startMessage = null;
					if( sql.trim().equalsIgnoreCase( "UPGRADE" ) )
						upgrade( patch, command );
					else
						execute( patch, command, count );
					this.callBack.executed();
				}
				else
					this.callBack.skipped( command );
			}

			command = this.patchFile.readStatement();
		}
		this.callBack.patchFinished();

		this.dbVersion.setStale(); // TODO With a normal patch, only set stale if not both of the 2 version tables are found
		if( patch.isInit() )
		{
			this.dbVersion.updateSpec( patch.getTarget() );
			Assert.isFalse( patch.isOpen() );
		}
		else
		{
			if( patch.isDowngrade() )
			{
				Set versions = this.patchFile.getReachableVersions( patch.getTarget(), null, false );
				versions.remove( patch.getTarget() );
				this.dbVersion.downgradeHistory( versions );
			}
			if( !patch.isOpen() )
			{
				this.dbVersion.updateVersion( patch.getTarget() );
				this.dbVersion.logComplete( patch.getSource(), patch.getTarget(), count );
			}
		}
	}

	private void upgrade( Patch patch, Command command ) throws SQLExecutionException
	{
		Assert.isFalse( !patch.isInit(), "UPGRADE only allowed in INIT blocks" );
		Assert.isTrue( patch.getSource().equals( "1.0" ) && patch.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		int pos = command.getLineNumber();
		execute( patch, new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false, pos ), 0 );
		execute( patch, new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETE' WHERE RESULT LIKE 'COMPLETED VERSION %'", false, pos ), 0 );
		execute( patch, new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false, pos ), 0 ); // We need this because the column is made NOT NULL in the upgrade init block
	}

	private void setConnection( Database database )
	{
		this.currentDatabase = database;
		database.init(); // Reset the current user
	}

	protected void setUser( String user )
	{
		this.currentDatabase.setCurrentUser( user );
	}

	protected void pushIgnores( String ignores )
	{
		String[] ss = ignores.split( "," );
		for( int i = 0; i < ss.length; i++ )
			ss[ i ] = ss[ i ].trim();
		this.ignoreStack.push( ss );
		refreshIgnores();
	}

	protected void popIgnores()
	{
		this.ignoreStack.pop();
		refreshIgnores();
	}

	protected void refreshIgnores()
	{
		HashSet ignores = new HashSet();
		for( Iterator iter = this.ignoreStack.iterator(); iter.hasNext(); )
		{
			String[] ss = (String[])iter.next();
			for( int i = 0; i < ss.length; i++ )
				ignores.add( ss[ i ] );
		}
		this.ignoreSet = ignores;
	}

	protected void enableDontCount()
	{
		Assert.isFalse( this.dontCount, "Counting already enabled" );
		this.dontCount = true;
	}

	protected void disableDontCount()
	{
		Assert.isTrue( this.dontCount, "Counting already disabled" );
		this.dontCount = false;
	}

	private void ifHistoryContains( String not, String version )
	{
		if( this.conditionFalseCounter == 0 )
		{
			boolean c = this.dbVersion.logContains( version );
			if( not != null )
				c = !c;
			if( c )
				this.conditionTrueCounter++;
			else
				this.conditionFalseCounter++;
		}
		else
			this.conditionFalseCounter++;
	}

	private void ifHistoryContainsEnd()
	{
		if( this.conditionFalseCounter > 0 )
			this.conditionFalseCounter--;
		else
		{
			Assert.isTrue( this.conditionTrueCounter > 0 );
			this.conditionTrueCounter--;
		}
	}

	public ProgressListener getCallBack()
	{
		return this.callBack;
	}

	public void setCallBack( ProgressListener callBack )
	{
		this.callBack = callBack;
	}

	public void logToXML( OutputStream out )
	{
		this.dbVersion.logToXML( out, Charset.forName( "UTF-8" ) );
	}

	public void logToXML( String filename )
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
	public void end()
	{
		closePatchFile();
		if( this.currentDatabase != null )
			this.currentDatabase.closeConnections();
	}

	protected void selectConnection( String name )
	{
		name = name.toLowerCase();
		Database database = this.databases.get( name );
		Assert.notNull( database, "Database '" + name + "' (case-insensitive) not known" );
		setConnection( database );
	}

	public void addConnection( solidbase.config.Connection connection )
	{
		Assert.notNull( this.defaultDatabase );
		String driver = connection.getDriver();
		String url = connection.getUrl();
		this.databases.put( connection.getName(), new Database( driver != null ? driver : this.defaultDatabase.driverName, url != null ? url : this.defaultDatabase.url, connection.getUser().toLowerCase(), connection.getPassword(), this.callBack ) );
	}

	public void connect()
	{
		this.defaultDatabase.getConnection();
	}
}
