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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;


/**
 * This class is the coordinator. It requests an upgrade path from the {@link PatchFile}, and reads commands from it. It
 * calls the {@link CommandListener}s, updates the DBVERSION and DBVERSIONLOG tables through {@link DBVersion}, calls
 * the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Patcher
{
	// Don't need whitespace at the end of the Patterns

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

	// The fields below are all part of the upgrade context. It's reset at the start of each change package.

	/**
	 * The message that should be shown when a statement is executed.
	 */
	protected String startMessage;

	/**
	 * Errors that should be ignored. @{link #ignoreSet} is kept in sync with this stack.
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
	protected ProgressListener progress;

	/**
	 * The upgrade file being executed.
	 */
	protected PatchFile patchFile;

	/**
	 * The default database. At the start of each change package, this database is put into {@link Patcher#currentDatabase} to become the current database.
	 */
	protected Database defaultDatabase;

	/**
	 * The current database. Gets reset to the default database at the start of each change package.
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
	 * @param listener Listens to the progress.
	 */
	public Patcher( ProgressListener listener )
	{
		this.progress = listener;

		this.databases = new HashMap< String, Database >();

		this.listeners = new ArrayList();
		this.listeners.add( new AssertCommandExecuter() );
		this.listeners.add( new ImportCSVListener() );

		reset();
	}

	/**
	 * Construct a new instance of the patcher.
	 * 
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public Patcher( ProgressListener listener, Database database )
	{
		this( listener );
		addDatabase( "default", database );
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
				this.progress.openingPatchFile( url );
				ralr = new RandomAccessLineReader( url );
			}
			else
			{
				File file = new File( baseDir, fileName ); // In the current folder
				this.progress.openingPatchFile( file );
				ralr = new RandomAccessLineReader( file );
			}

			this.patchFile = new PatchFile( ralr );

			this.progress.openedPatchFile( this.patchFile );

			try
			{
				this.patchFile.read();
			}
			catch( RuntimeException e )
			{
				// When open() fails, it should cleanup after itself.
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
		Assert.notNull( this.dbVersion, "default database may not have been configured" );
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
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
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

	/**
	 * Patch to the given target.
	 * 
	 * @param target The target to patch to.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void patch( String target ) throws SQLExecutionException
	{
		patch( target, false );
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target The target requested.
	 * @param downgradeable Indicates that downgrade paths are allowed to reach the given target.
	 * @throws SQLExecutionException When the execution of a command throws an {@link SQLException}.
	 */
	public void patch( String target, boolean downgradeable ) throws SQLExecutionException
	{
		init();

		if( target == null )
		{
			LinkedHashSet< String > targets = getTargets( true, null, downgradeable );
			if( targets.size() > 1 )
				throw new FatalException( "More than one possible target found, you should specify a target." );
			if( targets.size() == 0 )
				throw new SystemException( "Expected at least some targets" );

			String t = targets.iterator().next();
			patch( this.dbVersion.getVersion(), t, downgradeable );
			this.progress.patchingFinished();
			return;
		}

		LinkedHashSet< String > targets;

		if( target.endsWith( "*" ) )
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
		}
		else
		{
			targets = getTargets( false, null, downgradeable );
			for( String t : targets )
				if( ObjectUtils.equals( t, target ) )
				{
					patch( this.dbVersion.getVersion(), t, downgradeable );
					break;
				}
		}

		terminateCommandListeners();

		if( targets.size() == 0 )
			throw new FatalException( "There is no upgrade path from the current version of the database (" + StringUtils.defaultString( this.dbVersion.getVersion(), "no version" ) + ") to the requested target version " + target );

		this.progress.patchingFinished();
	}

	/**
	 * Gives listeners a chance to cleanup.
	 */
	protected void terminateCommandListeners()
	{
		for( CommandListener listener : this.listeners )
			listener.terminate();
	}

	/**
	 * Upgrade the database from the given version to the given target.
	 * 
	 * @param version The current version of the database.
	 * @param target The target to upgrade to.
	 * @param downgradeable Allow downgrades to reach the target
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void patch( String version, String target, boolean downgradeable ) throws SQLExecutionException
	{
		if( target.equals( version ) )
			return;

		Path path = this.patchFile.getPatchPath( version, target, downgradeable );
		Assert.isTrue( path.size() > 0, "No upgrades found" );

		for( Patch patch : path )
			patch( patch );
	}

	/**
	 * Give the listeners a chance to react to the given command.
	 * 
	 * @param command The command to be executed.
	 * @return True if a listener has processed the command, false otherwise.
	 * @throws SQLException If the database throws an exception.
	 */
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

	/**
	 * Execute the given command, coming from the given patch.
	 * 
	 * @param patch The patch that contains the command to be executed.
	 * @param command The command to be executed.
	 * @param count The number of the command within the complete patch. Will be 0 for transient commands.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
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
					this.progress.exception( command );
					throw new SQLExecutionException( command, e );
				}
				this.dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
				String error = e.getSQLState();
				if( !this.ignoreSet.contains( error ) )
				{
					this.progress.exception( command );
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

	/**
	 * Execute a patch.
	 * 
	 * @param patch The patch to be executed.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void patch( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch, "patch == null" );

		this.progress.patchStarting( patch );

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
						this.progress.exception( command );
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
						popCondition();
					else if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
						selectConnection( matcher.group( 1 ) );
					else
						throw new FatalException( "Unknown command " + sql + ", at line " + command.getLineNumber() );
				}
			}
			else if( this.dontCount )
			{
				this.progress.executing( command, this.startMessage );
				this.startMessage = null;
				execute( patch, command, 0 );
				this.progress.executed();
			}
			else
			{
				count++;
				if( count > skip && this.conditionFalseCounter == 0 )
				{
					this.progress.executing( command, this.startMessage );
					this.startMessage = null;
					if( sql.trim().equalsIgnoreCase( "UPGRADE" ) )
						upgrade( patch, command );
					else
						execute( patch, command, count );
					this.progress.executed();
				}
				else
					this.progress.skipped( command );
			}

			command = this.patchFile.readStatement();
		}
		this.progress.patchFinished();

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
		if( database != null )
			database.init(); // Reset the current user TODO Create a test for this.
	}

	/**
	 * Changes the current user. The database will stay constant.
	 * 
	 * @param user The user to make current.
	 */
	protected void setUser( String user )
	{
		this.currentDatabase.setCurrentUser( user );
	}

	/**
	 * Adds a comma separated list of SQLStates to be ignored. See {@link SQLException#getSQLState()}.
	 * 
	 * @param ignores A comma separated list of errors to be ignored.
	 */
	protected void pushIgnores( String ignores )
	{
		String[] ss = ignores.split( "," );
		for( int i = 0; i < ss.length; i++ )
			ss[ i ] = ss[ i ].trim();
		this.ignoreStack.push( ss );
		refreshIgnores();
	}

	/**
	 * Remove the last added list of ignores.
	 */
	protected void popIgnores()
	{
		this.ignoreStack.pop();
		refreshIgnores();
	}

	/**
	 * Synchronize the set of ignores with the queue's contents.
	 */
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

	/**
	 * Persistent commands should be considered transient.
	 */
	protected void enableDontCount()
	{
		Assert.isFalse( this.dontCount, "Counting already enabled" );
		this.dontCount = true;
	}

	/**
	 * Persistent commands should be considered persistent again.
	 */
	protected void disableDontCount()
	{
		Assert.isTrue( this.dontCount, "Counting already disabled" );
		this.dontCount = false;
	}

	/**
	 * If history does not contain the given version then start skipping the persistent commands. If <code>not</code> is true then this logic is reversed.
	 * 
	 * @param not True causes reversed logic.
	 * @param version The version to look for in the history.
	 */
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

	/**
	 * Pop a condition from the stack. If only true conditions remain, skipping of persistent commands is terminated.
	 */
	private void popCondition()
	{
		if( this.conditionFalseCounter > 0 )
			this.conditionFalseCounter--;
		else
		{
			Assert.isTrue( this.conditionTrueCounter > 0 );
			this.conditionTrueCounter--;
		}
	}

	/**
	 * Returns the progress listener.
	 * 
	 * @return The progress listener.
	 */
	public ProgressListener getCallBack()
	{
		return this.progress;
	}

	/**
	 * Sets the progress listener.
	 * 
	 * @param callBack The progress listener.
	 */
	public void setCallBack( ProgressListener callBack )
	{
		this.progress = callBack;
	}

	/**
	 * Write the history to the given output stream.
	 * 
	 * @param out The output stream to write to.
	 */
	public void logToXML( OutputStream out )
	{
		this.dbVersion.logToXML( out, Charset.forName( "UTF-8" ) );
	}

	/**
	 * Write the history to the given file name.
	 * 
	 * @param filename The file name to write to.
	 */
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

	/**
	 * Closes open files and closes connections.
	 */
	// TODO No signal to the listeners here?
	public void end()
	{
		closePatchFile();
		if( this.currentDatabase != null )
			this.currentDatabase.closeConnections();
	}

	/**
	 * Makes current another configured connection.
	 * 
	 * @param name The name of the connection to select.
	 */
	protected void selectConnection( String name )
	{
		name = name.toLowerCase();
		Database database = this.databases.get( name );
		Assert.notNull( database, "Database '" + name + "' (case-insensitive) not known" );
		setConnection( database );
	}

	/**
	 * Add a database.
	 * 
	 * @param name The name of the database.
	 * @param database The database.
	 */
	public void addDatabase( String name, Database database )
	{
		this.databases.put( name, database );

		if( name.equals( "default" ) )
		{
			this.defaultDatabase = database;
			setConnection( database ); // Also resets the current user for the connection
			this.dbVersion = new DBVersion( database, this.progress );
		}
	}

	/**
	 * Initializes the default connection.
	 */
	public void connect()
	{
		this.defaultDatabase.getConnection();
	}

	/**
	 * Returns a statement of the current version of the database in a user presentable form.
	 * 
	 * @return A statement of the current version of the database in a user presentable form.
	 */
	public String getVersionStatement()
	{
		return this.dbVersion.getVersionStatement();
	}
}
