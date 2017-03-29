/*--
 * Copyright 2011 Ren� M. de Bloois
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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import funny.Symbol;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.script.scopes.DefaultScope;
import solidstack.script.scopes.Scope;



/**
 * Execution context.
 *
 * @author Ren� M. de Bloois
 * @since Aug 2011
 */
abstract public class CommandContext
{
	static public enum CommitStrategy { AUTOCOMMIT, TRANSACTIONAL };

	/**
	 * The parent execution context.
	 */
	private CommandContext parent;

	/**
	 * All configured databases.
	 */
	private DatabaseContext databases;

	/**
	 * Is JDBC escape processing enabled or not?
	 */
	private boolean jdbcEscaping;

	/**
	 * Current section nesting.
	 */
	private int sectionLevel;

	/**
	 * Errors that should be ignored. @{link #ignoreSet} is kept in sync with this stack.
	 */
	private Stack< String[] > ignoreStack;

	/**
	 * Errors that should be ignored. This set is kept in sync with the {@link #ignoreStack}.
	 */
	private Set< String > ignoreSet;

	/**
	 * The current database.
	 */
	private Database currentDatabase;

	/**
	 * Together with {@link UpgradeProcessor#skipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link UpgradeProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link UpgradeProcessor#skipCounter} get incremented.
	 */
	private int noSkipCounter;

	/**
	 * Together with {@link UpgradeProcessor#noSkipCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link UpgradeProcessor#noSkipCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link UpgradeProcessor#skipCounter} get incremented.
	 */
	private int skipCounter;

	private boolean scriptExpansion;

	private CommitStrategy commitStrategy;

	/**
	 * The scripting scope.
	 */
	private Scope scope;


	/**
	 * Constructor.
	 */
	public CommandContext()
	{
		this.jdbcEscaping = false;
		this.sectionLevel = 0;
		this.ignoreStack = new Stack< String[] >();
		this.ignoreSet = new HashSet< String >();
		this.noSkipCounter = this.skipCounter = 0;
	}

	/**
	 * Constructs a child context.
	 *
	 * @param parent The parent context.
	 */
	public CommandContext( CommandContext parent )
	{
		// inherit
		this.parent = parent;
		this.databases = parent.databases;
		this.jdbcEscaping = parent.jdbcEscaping;
		this.sectionLevel = parent.sectionLevel;
		this.currentDatabase = parent.currentDatabase;
		// TODO Inherit scope from parent?

		// no inherit
		this.ignoreStack = new Stack< String[] >();
		this.ignoreSet = new HashSet< String >();
		this.noSkipCounter = this.skipCounter = 0;
	}

	/**
	 * Skip persistent commands depending on the boolean parameter. If the skip parameter is true commands will be
	 * skipped, otherwise not. As {@link #skip(boolean)} and {@link #endSkip(SourceLocation)} can be nested, the same number of
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
	 * Process the ELSE annotation.
	 *
	 * @param location The location where the ELSE is encountered.
	 */
	protected void doElse( SourceLocation location )
	{
		if( this.noSkipCounter <= 0 && this.skipCounter <= 0 )
			throw new SourceException( "ELSE without IF encountered", location );
		boolean skip = this.skipCounter > 0;
		endSkip( location );
		skip( !skip );
	}

	/**
	 * Process the /IF annotation.
	 *
	 * @param location The location where the END IF is encountered.
	 */
	protected void endIf( SourceLocation location )
	{
		if( this.noSkipCounter <= 0 && this.skipCounter <= 0 )
			throw new SourceException( "/IF without IF encountered", location );
		endSkip( location );
	}

	/**
	 * Stop skipping commands. As {@link #skip(boolean)} and {@link #endSkip(SourceLocation)} can be nested, only when the same number
	 * of endSkips are called as the number of skips, the skipping will stop.
	 *
	 * @param location The location where the END SKIP is encountered.
	 */
	protected void endSkip( SourceLocation location )
	{
		if( this.skipCounter > 0 )
			this.skipCounter--;
		else
		{
			if( this.noSkipCounter <= 0 )
				throw new SourceException( "/SKIP without SKIP encountered", location );
			this.noSkipCounter--;
		}
	}

	/**
	 * Are commands to be skipped?
	 *
	 * @return True if commands need to be skipped, false otherwise.
	 */
	public boolean skipping()
	{
		return this.skipCounter > 0 || this.parent != null && this.parent.skipping();
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
		HashSet< String > ignores = new HashSet< String >();
		for( String[] ss : this.ignoreStack )
			ignores.addAll( Arrays.asList( ss ) );
		this.ignoreSet = ignores;
	}

	/**
	 * Should the given error be ignored?
	 *
	 * @param error SQL Error code.
	 * @return True if the given error should be ignored, false otherwise.
	 */
	public boolean ignoreSQLError( String error )
	{
		return this.ignoreSet.contains( error ) || this.parent != null && this.parent.ignoreSQLError( error );
	}

	public Scope getScope()
	{
		if( this.scope == null )
		{
			this.scope = new DefaultScope();
			this.scope.val( Symbol.apply( "db" ), new ScriptDB( this ) ); // TODO Or var?
		}
		return this.scope;
	}

	public Scope swapScope( Scope scope )
	{
		Scope ret = this.scope;
		this.scope = scope;
		return ret;
	}

	/**
	 * Enable or disable JDBC escape processing.
	 *
	 * @param escaping True enables, false disables.
	 */
	public void setJdbcEscaping( boolean escaping )
	{
		this.jdbcEscaping = escaping;
	}

	/**
	 * Is JDBC escape processing enabled?
	 *
	 * @return True if JDBC escape processing is enabled, false otherwise.
	 */
	public boolean isJdbcEscaping()
	{
		return this.jdbcEscaping;
	}

	/**
	 * @return True if script expansion is on.
	 */
	public boolean isScriptExpansion()
	{
		return this.scriptExpansion;
	}

	/**
	 * Enable or disable script expansion. Script expansion is the execution of scripts inside placeholders in between
	 * the commands and inserting the resulting value.
	 *
	 * @param scriptExpansion True enables script expansion, false disables it.
	 */
	public void setScriptExpansion( boolean scriptExpansion )
	{
		this.scriptExpansion = scriptExpansion;
	}

	/**
	 * Set all configured databases.
	 *
	 * @param databases All configured databases.
	 */
	public void setDatabases( DatabaseContext databases )
	{
		this.databases = databases;
	}

	/**
	 * Returns all configured databases.
	 *
	 * @return All configured databases.
	 */
	public Collection< Database > getDatabases()
	{
		return this.databases.getDatabases();
	}

	/**
	 * Returns the database with the given name.
	 *
	 * @param name The name of the database.
	 * @return The database with the given name.
	 */
	public Database getDatabase( String name )
	{
		return this.databases.getDatabase( name );
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
	 * Sets the current database.
	 *
	 * @param database The database.
	 */
	public void setCurrentDatabase( Database database )
	{
		this.currentDatabase = database;
	}

	/**
	 * Returns the parent context.
	 *
	 * @return The parent context.
	 */
	public CommandContext getParent()
	{
		return this.parent;
	}

	/**
	 * Returns the section nesting level.
	 *
	 * @return The section nesting level.
	 */
	public int getSectionLevel()
	{
		return this.sectionLevel;
	}

	/**
	 * Sets the section nesting level.
	 *
	 * @param level The section nesting level.
	 */
	public void setSectionLevel( int level )
	{
		this.sectionLevel = level;
	}

	public void end()
	{
		for( Database database : getDatabases() )
			database.closeConnections();
	}

	public CommitStrategy commitStrategy()
	{
		return this.commitStrategy;
	}

	public void setCommitStrategy( CommitStrategy strategy )
	{
		this.commitStrategy = strategy;
	}

	/**
	 * Are we in transient mode?
	 *
	 * @return True if in transient mode, false otherwise.
	 */
	public boolean isTransient()
	{
		return false;
	}
}
