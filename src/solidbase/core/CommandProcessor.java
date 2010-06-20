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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Delimiter.Type;



/**
 * Processes and executes commands through JDBC, maintains state, triggers the listeners.
 * 
 * @author René M. de Bloois
 * @since May 2010
 */
public class CommandProcessor
{
	// Don't need whitespace at the end of the Patterns

	/**
	 * Pattern for IGNORE SQL ERROR.
	 */
	static protected final Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE\\s+SQL\\s+ERROR\\s+(\\w+(\\s*,\\s*\\w+)*)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /IGNORE SQL ERROR.
	 */
	static protected final Pattern ignoreEnd = Pattern.compile( "/IGNORE\\s+SQL\\s+ERROR", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SET USER.
	 */
	static protected final Pattern setUserPattern = Pattern.compile( "SET\\s+USER\\s+(\\w+)\\s*", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SELECT CONNECTION.
	 */
	static protected final Pattern selectConnectionPattern = Pattern.compile( "SELECT\\s+CONNECTION\\s+(\\w+)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SET MESSAGE.
	 */
	static protected final Pattern startMessagePattern = Pattern.compile( "(?:SET\\s+MESSAGE|MESSAGE\\s+START)\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for DELIMITER.
	 */
	static protected final Pattern delimiterPattern = Pattern.compile( "(?:SET\\s+DELIMITER|DELIMITER\\s+IS)(?:\\s+(ISOLATED)|\\s+(TRAILING))?\\s+(\\S+)(?:\\sOR(?:\\s+(ISOLATED)|\\s+(TRAILING))?\\s+(\\S+))?", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SECTION.
	 */
	static protected final Pattern sectionPattern = Pattern.compile( "SECTION(?:\\.(\\d))?\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE );

	/**
	 * The command reader.
	 */
	protected CommandSource commandSource;

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	protected List< CommandListener > listeners;

	// The fields below are all part of the execution context. It's reset at the start of each change package.

	/**
	 * The message that should be shown when a statement is executed.
	 */
	protected String startMessage;

	/**
	 * Current section nesting.
	 */
	protected int sectionLevel;

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
	public CommandProcessor( ProgressListener listener )
	{
		this.progress = listener;
		this.databases = new HashMap< String, Database >();
		this.listeners = PluginManager.getListeners();

		reset();
	}

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public CommandProcessor( ProgressListener listener, Database database )
	{
		this( listener );
		addDatabase( database );
	}

	/**
	 * Resets the execution context.
	 */
	protected void reset()
	{
		this.startMessage = null;
		this.ignoreStack = new Stack();
		this.ignoreSet = new HashSet();
		setConnection( getDefaultDatabase() );
		if( this.commandSource != null ) // TODO Still need this check?
			this.commandSource.setDelimiters( null );
	}

	/**
	 * Sets the SQL file to process.
	 * 
	 * @param commandSource The command reader.
	 */
	public void setCommandSource( CommandSource commandSource )
	{
		this.commandSource = commandSource;
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
	 * Execute the given command.
	 * 
	 * @param command The command to be executed.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void executeWithListeners( Command command ) throws SQLExecutionException
	{
		if( command.isPersistent() )
		{
			this.progress.executing( command, this.startMessage );
			this.startMessage = null;
		}

		try
		{
			if( !executeListeners( command ) )
				execute( command );
		}
		catch( SQLException e )
		{
			String error = e.getSQLState();
			if( !this.ignoreSet.contains( error ) )
			{
				SQLExecutionException newException = new SQLExecutionException( command, e );
				this.progress.exception( newException );
				throw newException;
			}
		}

		if( command.isPersistent() )
			this.progress.executed();
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
			if( listener.execute( this, command ) )
				return true;
		}
		return false;
	}

	/**
	 * Executes the command.
	 * 
	 * @param command The command to be executed.
	 * @throws SQLException Whenever an SQLException is thrown from JDBC.
	 */
	protected void execute( Command command ) throws SQLException
	{
		if( command.isTransient() )
		{
			String sql = command.getCommand();
			Matcher matcher;
			if( ( matcher = sectionPattern.matcher( sql ) ).matches() )
				section( matcher.group( 1 ), matcher.group( 2 ), command );
			else if( ( matcher = startMessagePattern.matcher( sql ) ).matches() )
				this.startMessage = matcher.group( 1 );
			else if( ( matcher = delimiterPattern.matcher( sql ) ).matches() )
				delimiter( matcher );
			else if( ( matcher = ignoreSqlErrorPattern.matcher( sql ) ).matches() )
				pushIgnores( matcher.group( 1 ) );
			else if( ignoreEnd.matcher( sql ).matches() )
				popIgnores();
			else if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
				selectConnection( matcher.group( 1 ), command );
			else if( ( matcher = setUserPattern.matcher( sql ) ).matches() )
				setUser( matcher.group( 1 ) );
			else
				throw new CommandFileException( "Unknown command " + sql, command.getLineNumber() );
		}
		else
		{
			jdbcExecute( command );
		}
	}

	/**
	 * Execute the given command.
	 * 
	 * @param command The command to be executed.
	 * @throws SQLException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void jdbcExecute( Command command ) throws SQLException
	{
		Assert.isTrue( command.isPersistent() ); // TODO Why?

		String sql = command.getCommand();
		if( sql.length() == 0 )
			return;

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

	/**
	 * Sets the current database and initializes it.
	 * 
	 * @param database The database to make current.
	 */
	protected void setConnection( Database database )
	{
		this.currentDatabase = database;
		if( database != null )
			database.init(); // Reset the current user TODO Create a test for this.
	}

	/**
	 * Changes the current user on the current database.
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
	 * Starts a new section. Calls {@link #startSection(int, String)} to actually pass it on to the {@link ProgressListener}.
	 * 
	 * @param level The level of the section.
	 * @param message The message to be shown.
	 * @param command The command that started this.
	 */
	protected void section( String level, String message, Command command )
	{
		int l = level != null ? Integer.parseInt( level ) : 1;
		if( l < 0 || l > 9 )
			throw new CommandFileException( "Section level must be 0..9", command.getLineNumber() );
		if( l > this.sectionLevel + 1 )
			throw new CommandFileException( "Section levels can't be skipped, current section level is " + this.sectionLevel, command.getLineNumber() );
		this.sectionLevel = l;
		startSection( l, message );
	}

	/**
	 * Starts a new section. Called by {@link #section(String, String, Command)}.
	 * 
	 * @param level The level of the section.
	 * @param message The message to be shown.
	 */
	protected void startSection( int level, String message )
	{
		this.progress.startSection( level, message );
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
		if( this.currentDatabase != null )
			this.currentDatabase.closeConnections();
		this.commandSource.close(); // TODO Unit test
	}

	/**
	 * Makes current another configured connection.
	 * 
	 * @param name The name of the connection to select.
	 * @param command The command that started this.
	 */
	protected void selectConnection( String name, Command command )
	{
		name = name.toLowerCase();
		Database database = this.databases.get( name );
		if( database == null )
			throw new CommandFileException( "Database '" + name + "' not configured", command.getLineNumber() );
		setConnection( database );
	}

	/**
	 * Parses delimiters.
	 * 
	 * @param matcher The matcher.
	 * @return The parsed delimiters.
	 */
	static protected Delimiter[] parseDelimiters( Matcher matcher )
	{
		Delimiter[] delimiters = new Delimiter[ matcher.group( 6 ) != null ? 2 : 1 ];
		for( int i = 0; i < delimiters.length; i++ )
		{
			int j = i * 3 + 3;
			String delimiter = matcher.group( j );
			j -= 2;
			Delimiter.Type type = Type.FREE;
			if( matcher.group( j++ ) != null )
				type = Type.ISOLATED;
			else if( matcher.group( j ) != null )
				type = Type.TRAILING;
			delimiters[ i ] = new Delimiter( delimiter, type );
		}
		return delimiters;
	}

	/**
	 * Overrides the current delimiter.
	 * 
	 * @param matcher The command matcher.
	 */
	protected void delimiter( Matcher matcher )
	{
		this.commandSource.setDelimiters( parseDelimiters( matcher ) );
	}

	/**
	 * Add a database.
	 * 
	 * @param database The database.
	 */
	public void addDatabase( Database database )
	{
		this.databases.put( database.getName(), database );

		if( database.getName().equals( "default" ) )
			setConnection( database ); // Also resets the current user for the connection
	}

	/**
	 * Returns the current database.
	 * 
	 * @return The current database.
	 */
	public Database getCurrentDatabase()
	{
		return this.currentDatabase;
	}

	/**
	 * Returns the default database.
	 * 
	 * @return The default database.
	 */
	public Database getDefaultDatabase()
	{
		return this.databases.get( "default" );
	}
}
