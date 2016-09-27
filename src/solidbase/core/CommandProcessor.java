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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.CommandContext.COMMIT_STRATEGY;
import solidbase.core.Delimiter.Type;
import solidbase.util.Assert;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;
import solidstack.script.Script;
import solidstack.script.ScriptParser;
import solidstack.script.expressions.Expression;



/**
 * Processes commands, maintains state, triggers the listeners.
 *
 * @author Ren� M. de Bloois
 * @since May 2010
 */
abstract public class CommandProcessor
{
	// Don't need whitespace at the end of the Patterns

	/**
	 * Pattern for ENCODING.
	 */
	static protected final Pattern encodingPattern = Pattern.compile( "ENCODING\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for IGNORE SQL ERROR.
	 */
	static protected final Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE\\s+SQL\\s+ERROR\\s+(\\w+(\\s*,\\s*\\w+)*)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /IGNORE SQL ERROR.
	 */
	static protected final Pattern ignoreEnd = Pattern.compile( "END\\s+IGNORE|/IGNORE\\s+SQL\\s+ERROR", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SET USER.
	 */
	static protected final Pattern setUserPattern = Pattern.compile( "SET\\s+USER\\s+(\\w+)\\s*", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SELECT CONNECTION.
	 */
	static protected final Pattern selectConnectionPattern = Pattern.compile( "(?:USE|SELECT)\\s+CONNECTION\\s+(\\w+)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for DELIMITER.
	 */
	static protected final Pattern delimiterPattern = Pattern.compile( "(?:SET\\s+DELIMITER|DELIMITER\\s+IS)(?:\\s+(ISOLATED)|\\s+(TRAILING))?\\s+(\\S+)(?:\\sOR(?:\\s+(ISOLATED)|\\s+(TRAILING))?\\s+(\\S+))?", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for TERMINATOR.
	 */
	static protected final Pattern terminatorPattern = Pattern.compile( "SET\\s+TERMINATOR\\s*=\\s*(?:(SEPARATE)\\s+|(TRAILING)\\s+)?(\\S+)(?:\\s+OR\\s+(?:(SEPARATE)\\s+|(TRAILING)\\s+)?(\\S+))?", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for TERMINATOR.
	 */
	static protected final Pattern resetTerminatorPattern = Pattern.compile( "RESET\\s+TERMINATOR", Pattern.CASE_INSENSITIVE );

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
	static protected final Pattern skipEnd = Pattern.compile( "END\\s+SKIP|/SKIP", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for JDBC ESCAPE PROCESSING
	 */
	static protected final Pattern JDBC_ESCAPING = Pattern.compile( "JDBC\\s+ESCAPE\\s+PROCESSING\\s+(ON|OFF)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for IF VARIABLE.
	 */
	static protected final Pattern IF_SCRIPT_COMMAND = Pattern.compile( "IF\\s+SCRIPT\\s+(.*)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for ELSE.
	 */
	static protected Pattern elsePattern = Pattern.compile( "ELSE", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /IF.
	 */
	static protected Pattern ifEndPattern = Pattern.compile( "END\\s+IF|/IF", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for RUN.
	 */
	// TODO Newlines should be allowed
	static protected Pattern runPattern = Pattern.compile( "\\s*RUN\\s+\"(.*)\"\\s*", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SCRIPT EXPANSION.
	 */
	static protected Pattern SCRIPT_EXPANSION_COMMAND = Pattern.compile( "SCRIPT\\s+EXPANSION\\s+(ON|OFF)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for SCRIPT.
	 */
	static protected Pattern SCRIPT_COMMAND = Pattern.compile( "SCRIPT(?:\\s+(.*))?", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for END SCRIPT.
	 */
	static protected Pattern END_SCRIPT_COMMAND = Pattern.compile( "--\\*\\s*END\\s+SCRIPT\\s*", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for COMMIT_STRATEGY.
	 */
	static protected Pattern SET_COMMIT_STRATEGY = Pattern.compile( "SET\\s+COMMIT_STRATEGY\\s+=\\s+(AUTOCOMMIT|TRANSACTIONAL)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for COMMIT_STRATEGY.
	 */
	static protected Pattern RESET_COMMIT_STRATEGY = Pattern.compile( "RESET\\s+COMMIT_STRATEGY", Pattern.CASE_INSENSITIVE );

	// TODO Commit pattern
//	static protected final Pattern commitPattern = Pattern.compile( "COMMIT", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for &{xxx} or &xxx placeholder.
	 */
	// TODO & or something else?
	// TODO Only with { }?
	static protected Pattern placeHolderPattern = Pattern.compile( "&(([A-Za-z\\$_][A-Za-z0-9\\$_]*)|\\{([A-Za-z\\$_][A-Za-z0-9\\$_]*)\\})" );

	/**
	 * Current execution context.
	 */
	protected CommandContext context;

	/**
	 * The progress listener.
	 */
	protected ProgressListener progress;

	/**
	 * Constructor.
	 *
	 * @param listener Listens to the progress.
	 */
	public CommandProcessor( ProgressListener listener )
	{
		this.progress = listener;
	}

	public CommandContext getContext()
	{
		return this.context;
	}

	/**
	 * Execute the given command.
	 *
	 * @param command The command to be executed.
	 * @param skip The command needs to be skipped.
	 * @return Whenever an {@link SQLException} is ignored.
	 * @throws ProcessException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected ProcessException executeWithListeners( Command command, boolean skip ) throws ProcessException
	{
		expand( command );

		if( command.isPersistent() )
			if( !skip )
				this.progress.executing( command );

		ProcessException result = null;
		try
		{
			if( !executeListeners( command, skip ) )
				if( !skip )
					if( command.isPersistent() )
						executeJdbc( command );
					else
						throw new SourceException( "Unknown command " + command.getCommand(), command.getLocation() );
		}
		catch( SQLException e )
		{
			// TODO Is this one thrown anymore?
			ProcessException newException = new ProcessException( e ).addProcess( "executing: " + command.getCommand() ).addLocation( command.getLocation() );
			String error = e.getSQLState();
			if( !this.context.ignoreSQLError( error ) )
			{
				this.progress.exception( newException );
				throw newException;
			}
			result = newException;
		}
		catch( ProcessException e )
		{
			// TODO Do we need to check SQLException to ignore?
			throw e.addProcess( "executing: " + command.getCommand() ).addLocation( command.getLocation() );
		}

		if( command.isPersistent() )
			if( !skip )
				this.progress.executed();
			else
				this.progress.skipped( command );

		return result;
	}

	/**
	 * Substitutes place holders in the command.
	 *
	 * @param command The command.
	 */
	protected void expand( Command command )
	{
		if( !this.context.isScriptExpansion() )
			return;

		// TODO Is this needed? The string parser is fast too.
		if( !command.getCommand().contains( "${" ) ) // TODO & or something else?
			return;

		Expression expression = ScriptParser.parseString( command.getCommand(), command.getLocation() );
		Object object = Script.eval( expression, this.context.getScope() );
		command.setCommand( object.toString() );

//		Template template = new TemplateCompiler( null ).compile( new StringResource( "<%@ template version=\"1.0\" language=\"javascript\" %>" + command.getCommand() ), null );
//		String result = template.apply( this.context.getScope() );
//		command.setCommand( result );
	}

	/**
	 * Give the listeners a chance to react to the given command.
	 *
	 * @param command The command to be executed.
	 * @param skip The command needs to be skipped.
	 * @return True if a listener has processed the command, false otherwise.
	 * @throws SQLException If the database throws an exception.
	 */
	protected boolean executeListeners( Command command, boolean skip ) throws SQLException
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
			if( ( matcher = delimiterPattern.matcher( sql ) ).matches() || ( matcher = terminatorPattern.matcher( sql ) ).matches() )
			{
				setDelimiters( parseDelimiters( matcher ) );
				return true;
			}
			if( resetTerminatorPattern.matcher( sql ).matches() )
			{
				setDelimiters( null );
				return true;
			}
			if( ( matcher = ignoreSqlErrorPattern.matcher( sql ) ).matches() )
			{
				this.context.pushIgnores( matcher.group( 1 ) );
				return true;
			}
			if( ignoreEnd.matcher( sql ).matches() )
			{
				this.context.popIgnores();
				return true;
			}
			if( ( matcher = selectConnectionPattern.matcher( sql ) ).matches() )
			{
				selectConnection( matcher.group( 1 ), command );
				return true;
			}
			if( ( matcher = IF_SCRIPT_COMMAND.matcher( sql ) ).matches() )
			{
				ifScript( matcher.group( 1 ), command );
				return true;
			}
			if( elsePattern.matcher( sql ).matches() )
			{
				this.context.doElse( command.getLocation() );
				return true;
			}
			if( ifEndPattern.matcher( sql ).matches() )
			{
				this.context.endIf( command.getLocation() );
				return true;
			}
			if( ( matcher = setUserPattern.matcher( sql ) ).matches() )
			{
				setUser( matcher.group( 1 ) );
				return true;
			}
			if( skipPattern.matcher( sql ).matches() )
			{
				this.context.skip( true );
				return true;
			}
			if( skipEnd.matcher( sql ).matches() )
			{
				this.context.endSkip( command.getLocation() );
				return true;
			}
			if( ( matcher = JDBC_ESCAPING.matcher( sql ) ).matches() )
			{
				this.context.setJdbcEscaping( matcher.group( 1 ).equalsIgnoreCase( "ON" ) );
				return true;
			}
			if( encodingPattern.matcher( sql ).matches() )
				// Ignore, already picked up by the EncodingDetector
				// TODO Check that it is the first line, and check with the detected encoding
				return true;
			if( ( matcher = SCRIPT_EXPANSION_COMMAND.matcher( sql ) ).matches() )
			{
				this.context.setScriptExpansion( "ON".equalsIgnoreCase( matcher.group( 1 ) ) );
				return true;
			}
			if( ( matcher = SCRIPT_COMMAND.matcher( sql ) ).matches() )
			{
				String script = matcher.group( 1 );
				if( script != null )
					script( script, command.getLocation() );
				else
				{
					SourceReader reader = getReader();
					StringBuilder buf = new StringBuilder();
					while( true )
					{
						String line = reader.readLine();
						if( line == null )
							throw new SourceException( "Missing END SCRIPT for script", command.getLocation() );
						if( END_SCRIPT_COMMAND.matcher( line ).matches() )
							break;
						buf.append( line ).append( '\n' );
					}
					script( buf.toString(), command.getLocation().nextLine() );
				}
				return true;
			}
			if( ( matcher = SET_COMMIT_STRATEGY.matcher( sql ) ).matches() )
			{
				this.context.setCommitStrategy( "AUTOCOMMIT".equalsIgnoreCase( matcher.group( 1 ) ) ? COMMIT_STRATEGY.AUTOCOMMIT : COMMIT_STRATEGY.TRANSACTIONAL );
				return true;
			}
			if( RESET_COMMIT_STRATEGY.matcher( sql ).matches() )
			{
				this.context.setCommitStrategy( null );
				return true;
			}
//			if( commitPattern.matcher( sql ).matches() )
//			{
//				getCurrentDatabase().getConnection().commit();
//				return true;
//			}
		}
		else if( !skip )
			if( ( matcher = runPattern.matcher( sql ) ).matches() )
			{
				run( matcher.group( 1 ) );
				return true;
			}

		for( CommandListener listener : PluginManager.listeners )
			if( listener.execute( this, command, skip ) )
				return true;

		return false;
	}

	/**
	 * Creates a new statement from the current connection. JDBC escape processing is enabled or disabled according to the current configuration.
	 *
	 * @return The statement.
	 * @throws SQLException Whenever JDBC throws an SQLException.
	 */
	// TODO Maybe we should wrap the connection and override the createStatement there.
	public Statement createStatement() throws SQLException
	{
		Connection connection = getCurrentDatabase().getConnection();
		connection.setAutoCommit( this.context.commitStrategy() == COMMIT_STRATEGY.AUTOCOMMIT );
		Statement statement = connection.createStatement();
		statement.setEscapeProcessing( this.context.isJdbcEscaping() );
		return statement;
	}

	/**
	 * Prepares a new statement from the current connection.

	 * @param sql The SQL for the statement.
	 * @return The prepared statement.
	 * @throws SQLException Whenever JDBC throws an SQLException.
	 */
	public PreparedStatement prepareStatement( String sql ) throws SQLException
	{
		Connection connection = getCurrentDatabase().getConnection();
		connection.setAutoCommit( this.context.commitStrategy() == COMMIT_STRATEGY.AUTOCOMMIT );
		try
		{
			return connection.prepareStatement( sql );
		}
		catch( SQLException e )
		{
			throw new ProcessException( e ).addProcess( "preparing the statement: " + sql );
		}

		// This does not work in Oracle: gives invalid character error
		// Apparently it will never work, because (in which JDBC drivers?) the SQL is already processed before this call.
//		statement.setEscapeProcessing( this.context.getJdbcEscaping() );
	}

	/**
	 * Closes the given statement and commits or rollbacks if the command processor is in auto commit mode.
	 *
	 * @param statement The statement to close.
	 * @param commitOrRollback If the command processor is in auto commit mode, this boolean indicates if commit or
	 *        rollback should be called on the statement's connection. If the command processor is not in auto commit
	 *        mode, this boolean is ignored.
	 */
	public void closeStatement( Statement statement, boolean commitOrRollback )
	{
		try
		{
			if( implicitCommit() && this.context.commitStrategy() != COMMIT_STRATEGY.AUTOCOMMIT )
			{
				Connection connection = statement.getConnection();
				if( commitOrRollback )
					connection.commit();
				else
					connection.rollback();
			}
			statement.close(); // TODO Shouldn't the statement be closed before commit or rollback?
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
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

		Statement statement = createStatement();
		boolean commit = false;
		try
		{
			statement.execute( sql );
			commit = true;
		}
		finally
		{
			closeStatement( statement, commit );
		}
	}

	/**
	 * Sets the current database and initializes it.
	 *
	 * @param database The database to make current.
	 */
	protected void setConnection( Database database )
	{
		this.context.setCurrentDatabase( database );
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
		this.context.getCurrentDatabase().setCurrentUser( user );
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
			throw new SourceException( "Section level must be 0..9", command.getLocation() );
		if( l > this.context.getSectionLevel() + 1 ) // TODO Why is this?
			throw new SourceException( "Section levels can't be skipped, current section level is " + this.context.getSectionLevel(), command.getLocation() );
		this.context.setSectionLevel( l );
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
		SQLFile file = Factory.openSQLFile( getResource().resolve( url ), this.progress );
		SQLProcessor processor = new SQLProcessor( this.progress );
		processor.setContext( new SQLContext( this.context, file.getSource() ) );
		processor.process();
	}

	protected Object script( String script, SourceLocation location )
	{
		SourceReader reader = SourceReaders.forString( script, location );
		return Script.compile( reader ).eval( this.context.getScope() );
	}

	/**
	 * Returns the progress listener.
	 *
	 * @return The progress listener.
	 */
	public ProgressListener getProgressListener()
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
	abstract public void end();

	/**
	 * Makes current another configured connection.
	 *
	 * @param name The name of the connection to select.
	 * @param command The command that started this.
	 */
	protected void selectConnection( String name, Command command )
	{
		name = name.toLowerCase();
		Database database = this.context.getDatabase( name );
		if( database == null )
			throw new SourceException( "Database '" + name + "' not configured", command.getLocation() );
		setConnection( database );
	}

	protected void ifScript( String script, Command command )
	{
		SourceReader reader = SourceReaders.forString( script, command.getLocation() );
		boolean condition = Script.compile( reader ).evalBoolean( this.context.getScope() );
		this.context.skip( !condition );
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
	 * Returns the current database.
	 *
	 * @return The current database.
	 */
	public Database getCurrentDatabase()
	{
		return this.context.getCurrentDatabase();
	}

	/**
	 * Returns the default database.
	 *
	 * @return The default database.
	 */
	public Database getDefaultDatabase()
	{
		return this.context.getDatabase( "default" );
	}

	/**
	 * Returns the {@link SourceReader} that is the source of the commands.
	 *
	 * @return the {@link SourceReader} that is the source of the commands.
	 */
	abstract public SourceReader getReader();

	/**
	 * Returns the underlying resource.
	 *
	 * @return The underlying resource.
	 */
	abstract public Resource getResource();

	/**
	 * If true ({@link UpgradeProcessor}), commands get committed automatically, and rolled back when an {@link SQLException} occurs.
	 * If false ({@link SQLProcessor}), commit/rollback should be in the command source.
	 *
	 * @return True if commands get committed or rollbacked automatically, false otherwise.
	 */
	abstract public boolean implicitCommit();
}
