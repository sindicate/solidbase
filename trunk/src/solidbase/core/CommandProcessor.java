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
import java.sql.ResultSet;
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
import solidbase.util.Assert;
import solidbase.util.LineReader;
import solidbase.util.Resource;



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
	 * Pattern for JDBC ESCAPE PROCESSING
	 */
	static protected final Pattern JDBC_ESCAPING = Pattern.compile( "JDBC\\s+ESCAPE\\s+PROCESSING\\s+(ON|OFF)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SET VARIABLE.
	 */
	static protected final Pattern setVariablePattern = Pattern.compile( "SET\\s+VARIABLE\\s+(\\w+)\\s*=\\s*(SELECT\\s+.*)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for IF VARIABLE.
	 */
	static protected final Pattern ifVariablePattern = Pattern.compile( "IF\\s+VARIABLE\\s+(\\w+)\\s+IS\\s+(NOT\\s+)?NULL", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for ELSE.
	 */
	static protected Pattern elsePattern = Pattern.compile( "ELSE", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /IF.
	 */
	static protected Pattern ifEndPattern = Pattern.compile( "/IF", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for RUN.
	 */
	// TODO Newlines should be allowed
	static protected Pattern runPattern = Pattern.compile( "RUN\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE );

	/**
	 * A list of command listeners. A listener listens to the statements being executed and is able to intercept specific ones.
	 */
	protected List< CommandListener > listeners;

	/**
	 * If true ({@link UpgradeProcessor}), commands get committed automatically, and rolled back when an {@link SQLException} occurs.
	 * If false ({@link SQLProcessor}), commit/rollback should be in the command source.
	 */
	protected boolean autoCommit;

	// The fields below are all part of the execution context. It's reset at the start of each command set.

	/**
	 * Is JDBC escape processing enabled or not?
	 */
	protected boolean jdbcEscaping;

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
	 * Together with {@link UpgradeProcessor#skipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link UpgradeProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link UpgradeProcessor#skipCounter} get incremented.
	 */
	protected int noSkipCounter;

	/**
	 * Together with {@link UpgradeProcessor#noSkipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link UpgradeProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link UpgradeProcessor#skipCounter} get incremented.
	 */
	protected int skipCounter;

	/**
	 * Variables. Null instead of empty.
	 */
	protected Map< String, String > variables;

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
	 * Construct a child command processor.
	 *
	 * @param parent The parent command processor.
	 */
	public CommandProcessor( CommandProcessor parent )
	{
		this.databases = parent.databases;
		this.progress = parent.progress;
		reset();
		this.currentDatabase = parent.currentDatabase;
		this.ignoreSet.addAll( parent.ignoreSet );
		this.ignoreStack.addAll( parent.ignoreStack );
		this.listeners = PluginManager.getListeners();
		// TODO Section depth needs to be offset
		if( parent.variables != null )
			this.variables = new HashMap< String, String >( parent.variables );
	}

	/**
	 * Resets the execution context.
	 */
	protected void reset()
	{
		this.jdbcEscaping = false;
		this.sectionLevel = 0;
		this.progress.reset();
		this.ignoreStack = new Stack< String[] >();
		this.ignoreSet = new HashSet< String >();
		this.noSkipCounter = this.skipCounter = 0;
		this.variables = null;
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
	 * @return Whenever an {@link SQLException} is ignored.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected SQLExecutionException executeWithListeners( Command command ) throws SQLExecutionException
	{
		substituteVariables( command );

		if( command.isPersistent() )
			this.progress.executing( command );

		SQLExecutionException result = null;
		try
		{
			if( !executeListeners( command ) )
				if( command.isPersistent() )
					executeJdbc( command );
				else
					throw new CommandFileException( "Unknown command " + command.getCommand(), command.getLineNumber() );
		}
		catch( SQLException e )
		{
			SQLExecutionException newException = new SQLExecutionException( command.getCommand(), command.getLineNumber(), e );
			String error = e.getSQLState();
			if( !this.ignoreSet.contains( error ) )
			{
				this.progress.exception( newException );
				throw newException;
			}
			result = newException;
		}

		if( command.isPersistent() )
			this.progress.executed();

		return result;
	}

	/**
	 * Substitutes place holders in the command with the values from the variables.
	 *
	 * @param command The command.
	 */
	protected void substituteVariables( Command command )
	{
		if( this.variables == null )
			return;
		if( !command.getCommand().contains( "&" ) )
			return;

		// TODO Maybe do a two-step when the command is very large (collect all first, replace only if found)
		Pattern pattern = Pattern.compile( "&(([A-Za-z\\$_][A-Za-z0-9\\$_]*)|\\{([A-Za-z\\$_][A-Za-z0-9\\$_]*)\\})" );
		Matcher matcher = pattern.matcher( command.getCommand() );
		StringBuffer sb = new StringBuffer();
		while( matcher.find() )
		{
			String name = matcher.group( 2 );
			if( name == null )
				name = matcher.group( 3 );
			name = name.toUpperCase();
			if( this.variables.containsKey( name ) )
			{
				String value = this.variables.get( name );
				if( value == null )
					throw new CommandFileException( "Variable '" + name + "' is null", command.getLineNumber() );
				matcher.appendReplacement( sb, value );
			}
		}
		matcher.appendTail( sb );
		command.setCommand( sb.toString() );
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
		String sql = command.getCommand();
		Matcher matcher;
		if( command.isTransient() )
		{
			if( ( matcher = sectionPattern.matcher( sql ) ).matches() )
			{
				section( matcher.group( 1 ), matcher.group( 2 ), command );
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
			if( ( matcher = setVariablePattern.matcher( sql ) ).matches() )
			{
				setVariableFromSelect( matcher.group( 1 ), matcher.group( 2 ) );
				return true;
			}
			if( ( matcher = ifVariablePattern.matcher( sql ) ).matches() )
			{
				ifVariableIsNull( matcher.group( 1 ), matcher.group( 2 ), command );
				return true;
			}
			if( elsePattern.matcher( sql ).matches() )
			{
				doElse();
				return true;
			}
			if( ifEndPattern.matcher( sql ).matches() )
			{
				endSkip();
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
			if( ( matcher = JDBC_ESCAPING.matcher( sql ) ).matches() )
			{
				this.jdbcEscaping = matcher.group( 1 ).equalsIgnoreCase( "ON" );
				return true;
			}
		}
		else
		{
			if( ( matcher = runPattern.matcher( sql ) ).matches() )
			{
				run( matcher.group( 1 ) );
				return true;
			}
		}

		for( CommandListener listener : this.listeners )
			if( listener.execute( this, command ) )
				return true;

		return false;
	}

	/**
	 * Creates a new statement. JDBC escape processing is enabled or disabled according to the current configuration.
	 *
	 * @param connection The connection to create a statement from.
	 * @return The statement.
	 * @throws SQLException Whenever JDBC throws an SQLException.
	 */
	// TODO Maybe we should wrap the connection and override the createStatement there.
	public Statement createStatement( Connection connection ) throws SQLException
	{
		Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
		Statement statement = connection.createStatement();
		statement.setEscapeProcessing( this.jdbcEscaping );
		return statement;
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

		Connection connection = this.currentDatabase.getConnection();
		Statement statement = createStatement( connection );
		boolean commit = false;
		try
		{
			statement.execute( sql );
			commit = true;
		}
		finally
		{
			statement.close();
			if( this.autoCommit )
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
	 * Process the ELSE annotation.
	 */
	protected void doElse()
	{
		boolean skip = this.skipCounter > 0;
		endSkip();
		skip( !skip );
	}

	/**
	 * Starts a new section.
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
		if( l > this.sectionLevel + 1 ) // TODO Why is this?
			throw new CommandFileException( "Section levels can't be skipped, current section level is " + this.sectionLevel, command.getLineNumber() );
		this.sectionLevel = l;
		startSection( l, message );
	}

	/**
	 * Starts a new section.
	 *
	 * @param level The level of the section.
	 * @param message The message to be shown.
	 */
	protected void startSection( int level, String message )
	{
		this.progress.startSection( level, message );
	}

	/**
	 * Runs a different SQL file.
	 *
	 * @param url The path of the SQL file.
	 */
	protected void run( String url )
	{
		SQLProcessor processor = new SQLProcessor( this );
		// TODO What if the protocol is different?
		SQLFile file = Factory.openSQLFile( getResource().createRelative( url ), this.progress );
		processor.setSQLSource( file.getSource() );
		processor.process();
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
		terminateCommandListeners();
		for( Database database : this.databases.values() )
			database.closeConnections();
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
	 * Execute the SELECT and set the variable with the result from the SELECT.
	 *
	 * @param name Name of the variable.
	 * @param select The SELECT SQL statement.
	 * @throws SQLException Whenever the database throws one.
	 */
	protected void setVariableFromSelect( String name, String select ) throws SQLException
	{
		Connection connection = this.currentDatabase.getConnection();
		Statement statement = createStatement( connection );
		Object value = null;
		try
		{
			ResultSet result = statement.executeQuery( select );
			if( result.next() )
				value = result.getObject( 1 );
		}
		finally
		{
			statement.close();
			if( this.autoCommit )
				connection.commit();
		}

		if( this.variables == null )
			this.variables = new HashMap< String, String >();
		this.variables.put( name.toUpperCase(), value == null ? null : value.toString() );
	}

	/**
	 * Process the IF VARIABLE IS [NOT] NULL annotation.
	 *
	 * @param name The name of the variable.
	 * @param not Is NOT part of the annotation?
	 * @param command The command itself needed for the line number if an exception is thrown.
	 */
	protected void ifVariableIsNull( String name, String not, Command command )
	{
		if( this.variables == null || !this.variables.containsKey( name ) )
			throw new CommandFileException( "Variable '" + name + "' is not defined", command.getLineNumber() );
		skip( ( this.variables.get( name ) == null ) != ( not == null ) );
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

	/**
	 * Returns the {@link LineReader} that is the source of the commands.
	 *
	 * @return the {@link LineReader} that is the source of the commands.
	 */
	abstract public LineReader getReader();

	/**
	 * Returns the underlying resource.
	 *
	 * @return The underlying resource.
	 */
	abstract public Resource getResource();
}
