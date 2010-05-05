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
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is the coordinator. It reads commands from the {@link SQLFile}. It calls the {@link CommandListener}s,
 * calls the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 * 
 * @author Ren� M. de Bloois
 * @since May 2010
 */
public class SQLProcessor
{
	// Don't need whitespace at the end of the Patterns

	static private Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE\\s+SQL\\s+ERROR\\s+(\\w+(\\s*,\\s*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static private Pattern ignoreEnd = Pattern.compile( "/IGNORE\\s+SQL\\s+ERROR", Pattern.CASE_INSENSITIVE );

	static private Pattern setUserPattern = Pattern.compile( "SET\\s+USER\\s+(\\w+)\\s*", Pattern.CASE_INSENSITIVE );
	static private Pattern selectConnectionPattern = Pattern.compile( "SELECT\\s+CONNECTION\\s+(\\w+)", Pattern.CASE_INSENSITIVE );

	static private Pattern startMessagePattern = Pattern.compile( "(?:SET\\s+MESSAGE|MESSAGE\\s+START)\\s+[\"](.*)[\"]", Pattern.CASE_INSENSITIVE );

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	protected List< CommandListener > listeners;

	// The fields below are all part of the execution context.

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
	 * The progress listener.
	 */
	protected ProgressListener progress;

	/**
	 * The sql file being executed.
	 */
	protected SQLFile sqlFile;

	/**
	 * The current database.
	 */
	protected Database currentDatabase;

	/**
	 * All configured databases. This is used when the upgrade file selects a different database by name.
	 */
	protected Map< String, Database > databases;

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 */
	public SQLProcessor( ProgressListener listener )
	{
		this.progress = listener;

		this.databases = new HashMap< String, Database >();

		this.listeners = new ArrayList();
		this.listeners.add( new AssertCommandExecuter() );
		this.listeners.add( new ImportCSVListener() );

		reset();
	}

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public SQLProcessor( ProgressListener listener, Database database )
	{
		this( listener );
		addDatabase( "default", database );
	}

	/**
	 * Initialize the sql executer.
	 * 
	 * @param baseDir The base folder from where to look for the upgrade file (optional).
	 * @param sqlFileName The name of the sql file.
	 */
	public void init( File baseDir, String sqlFileName )
	{
		openSQLFile( baseDir, sqlFileName );
	}

	/**
	 * Initialize the sql executer.
	 * 
	 * @param sqlFileName The name of the sql file.
	 */
	public void init( String sqlFileName )
	{
		init( null, sqlFileName );
	}

	/**
	 * Resets the execution context.
	 */
	protected void reset()
	{
		this.startMessage = null;
		this.ignoreStack = new Stack();
		this.ignoreSet = new HashSet();
	}

	/**
	 * Open the specified sql file.
	 * 
	 * @param fileName The name and path of the sql file.
	 */
	protected void openSQLFile( String fileName )
	{
		openSQLFile( null, fileName );
	}

	/**
	 * Open the specified sql file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the sql file.
	 */
	protected void openSQLFile( File baseDir, String fileName )
	{
		Assert.notNull( fileName );

		try
		{
			RandomAccessLineReader ralr;
			// TODO Should we remove this "/"?
			URL url = SQLProcessor.class.getResource( "/" + fileName ); // In the classpath
			if( url != null )
			{
				this.progress.openingSQLFile( url );
				ralr = new RandomAccessLineReader( url );
			}
			else
			{
				File file = new File( baseDir, fileName ); // In the current folder
				this.progress.openingSQLFile( file );
				ralr = new RandomAccessLineReader( file );
			}

			this.sqlFile = new SQLFile( ralr );

			this.progress.openedSQLFile( this.sqlFile );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Close the upgrade file.
	 */
	public void closeSQLFile()
	{
		if( this.sqlFile != null )
			this.sqlFile.close();
		this.sqlFile = null;
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
	 * Execute the given command.
	 * 
	 * @param command The command to be executed.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void execute( Command command ) throws SQLExecutionException
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
				String error = e.getSQLState();
				if( !this.ignoreSet.contains( error ) )
				{
					this.progress.exception( command );
					throw new SQLExecutionException( command, e );
				}
			}
		}
	}

	/**
	 * Execute the sql file.
	 * 
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void execute() throws SQLExecutionException
	{
		Command command = this.sqlFile.readStatement();
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
						String error = e.getSQLState();
						if( this.ignoreSet.contains( error ) )
							return;
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
					else if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
						selectConnection( matcher.group( 1 ) );
					else
						throw new FatalException( "Unknown command " + sql + ", at line " + command.getLineNumber() );
				}
			}
			else
			{
				this.progress.executing( command, this.startMessage );
				this.startMessage = null;
				execute( command );
				this.progress.executed();
			}

			command = this.sqlFile.readStatement();
		}
		this.progress.sqlExecutionComplete();
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
	 * Closes open files and closes connections.
	 */
	// TODO No signal to the listeners here?
	public void end()
	{
		closeSQLFile();
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
			setConnection( database ); // Also resets the current user for the connection
	}
}
