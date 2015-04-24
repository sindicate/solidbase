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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Delimiter.Type;



/**
 * Processes commands, maintains state, triggers the listeners.
 * 
 * @author René M. de Bloois
 * @since May 2010
 */
abstract public class CommandProcessor
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
	 * Pattern for SKIP.
	 */
	static protected final Pattern skipPattern = Pattern.compile( "SKIP", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /SKIP.
	 */
	static protected final Pattern skipEnd = Pattern.compile( "/SKIP", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for BATCH.
	 */
	static protected final Pattern batchPattern = Pattern.compile( "BATCH", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /BATCH.
	 */
	static protected final Pattern batchEnd = Pattern.compile( "/BATCH", Pattern.CASE_INSENSITIVE );

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	protected List< CommandListener > listeners;

	// The fields below are all part of the execution context. It's reset at the start of each command set.

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
	protected Stack< String[] > ignoreStack;

	/**
	 * Errors that should be ignored. This set is kept in sync with the {@link #ignoreStack}.
	 */
	protected Set< String > ignoreSet;

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
	 * Together with {@link PatchProcessor#skipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link PatchProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link PatchProcessor#skipCounter} get incremented.
	 */
	protected int noSkipCounter;

	/**
	 * Together with {@link PatchProcessor#noSkipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link PatchProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link PatchProcessor#skipCounter} get incremented.
	 */
	protected int skipCounter;

	/**
	 * Batch mode statement. Not null when batch mode is on, null otherwise.
	 */
	protected Statement batch;

	/**
	 * Constructor.
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
	 * Constructor.
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
		this.ignoreStack = new Stack< String[] >();
		this.ignoreSet = new HashSet< String >();
		this.noSkipCounter = this.skipCounter = 0;
		setConnection( getDefaultDatabase() );
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
			if( this.batch != null )
			{
				if( batchEnd.matcher( command.getCommand() ).matches() )
					endBatch();
				else
				{
					if( command.isTransient() )
						throw new CommandFileException( "Transient commands are not allowed during batch mode", command.getLineNumber() );
					executeJdbc( command );
				}
			}
			else
			{
				if( !executeListeners( command ) )
					if( command.isPersistent() )
						executeJdbc( command );
					else
						throw new CommandFileException( "Unknown command " + command.getCommand(), command.getLineNumber() );
			}
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
		if( command.isTransient() )
		{
			String sql = command.getCommand();
			Matcher matcher;
			if( ( matcher = sectionPattern.matcher( sql ) ).matches() )
			{
				section( matcher.group( 1 ), matcher.group( 2 ), command );
				return true;
			}
			if( ( matcher = startMessagePattern.matcher( sql ) ).matches() )
			{
				this.startMessage = matcher.group( 1 );
				return true;
			}
			if( ( matcher = delimiterPattern.matcher( sql ) ).matches() )
			{
				setDelimiters( parseDelimiters( matcher ) );
				return true;
			}
			if( ( matcher = ignoreSqlErrorPattern.matcher( sql ) ).matches() )
			{
				pushIgnores( matcher.group( 1 ) );
				return true;
			}
			if( ignoreEnd.matcher( sql ).matches() )
			{
				popIgnores();
				return true;
			}
			if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
			{
				selectConnection( matcher.group( 1 ), command );
				return true;
			}
			if( ( matcher = setUserPattern.matcher( sql ) ).matches() )
			{
				setUser( matcher.group( 1 ) );
				return true;
			}
			if( skipPattern.matcher( sql ).matches() )
			{
				skip( true );
				return true;
			}
			if( skipEnd.matcher( sql ).matches() )
			{
				endSkip();
				return true;
			}
			if( batchPattern.matcher( sql ).matches() )
			{
				startBatch();
				return true;
			}
		}

		for( CommandListener listener : this.listeners )
			if( listener.execute( this, command ) )
				return true;

		return false;
	}

	/**
	 * Execute the given command.
	 * 
	 * @param command The command to be executed.
	 * @throws SQLException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void executeJdbc( Command command ) throws SQLException
	{
		Assert.isTrue( command.isPersistent() ); // TODO Why?

		String sql = command.getCommand();
		if( sql.length() == 0 )
			return;

		if( this.batch != null )
		{
			this.batch.addBatch( sql );
		}
		else
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
	 * Skip persistent commands depending on the boolean parameter. If the skip parameter is true commands will be
	 * skipped, otherwise not. As {@link #skip(boolean)} and {@link #endSkip()} can be nested, the same number of
	 * endSkips need to be called as the number of skips to stop the skipping.
	 * 
	 * @param skip If true, commands will be skipped, otherwise not.
	 */
	protected void skip( boolean skip )
	{
		if( this.skipCounter == 0 )
		{
			if( skip )
				this.skipCounter++;
			else
				this.noSkipCounter++;
		}
		else
			this.skipCounter++;
	}

	/**
	 * Stop skipping commands. As {@link #skip(boolean)} and {@link #endSkip()} can be nested, only when the same number
	 * of endSkips are called as the number of skips, the skipping will stop.
	 */
	protected void endSkip()
	{
		if( this.skipCounter > 0 )
			this.skipCounter--;
		else
		{
			Assert.isTrue( this.noSkipCounter > 0 );
			this.noSkipCounter--;
		}
	}

	/**
	 * Batch mode starts.
	 * 
	 * @throws SQLException Whenever JDBC throws an {@link SQLException}.
	 */
	protected void startBatch() throws SQLException
	{
		Connection connection = this.currentDatabase.getConnection();
		Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
		this.batch = connection.createStatement();
	}

	/**
	 * Batch mode ends. The batch is executed.
	 * 
	 * @throws SQLException Whenever JDBC throws an {@link SQLException}.
	 */
	protected void endBatch() throws SQLException
	{
		boolean commit = false;
		try
		{
			this.batch.executeBatch();
			commit = true;
		}
		finally
		{
			Connection connection = this.batch.getConnection();
			this.batch.close();
			this.batch = null;
			if( commit )
				connection.commit();
			else
				connection.rollback();
		}
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
		HashSet< String > ignores = new HashSet< String >();
		for( String[] ss : this.ignoreStack )
			ignores.addAll( Arrays.asList( ss ) );
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
	 * Overrides the current delimiters.
	 * 
	 * @param delimiters The delimiters.
	 */
	abstract protected void setDelimiters( Delimiter[] delimiters );

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
