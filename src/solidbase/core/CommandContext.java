/*--
 * Copyright 2011 René M. de Bloois
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
 * @author René M. de Bloois
 * @since Aug 2011
 */
abstract public class CommandContext
{
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
	private Stack<String[]> ignoreStack;

	/**
	 * Errors that should be ignored. This set is kept in sync with the {@link #ignoreStack}.
	 */
	private Set<String> ignoreSet;

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
	public CommandContext() {
		jdbcEscaping = false;
		sectionLevel = 0;
		ignoreStack = new Stack<>();
		ignoreSet = new HashSet<>();
		noSkipCounter = skipCounter = 0;
	}

	/**
	 * Constructs a child context.
	 *
	 * @param parent The parent context.
	 */
	public CommandContext( CommandContext parent ) {
		// inherit
		this.parent = parent;
		databases = parent.databases;
		jdbcEscaping = parent.jdbcEscaping;
		sectionLevel = parent.sectionLevel;
		currentDatabase = parent.currentDatabase;
		// TODO Inherit scope from parent?

		// no inherit
		ignoreStack = new Stack<>();
		ignoreSet = new HashSet<>();
		noSkipCounter = skipCounter = 0;
	}

	/**
	 * Skip persistent commands depending on the boolean parameter. If the skip parameter is true commands will be
	 * skipped, otherwise not. As {@link #skip(boolean)} and {@link #endSkip(SourceLocation)} can be nested, the same
	 * number of endSkips need to be called as the number of skips to stop the skipping.
	 *
	 * @param skip If true, commands will be skipped, otherwise not.
	 */
	protected void skip( boolean skip ) {
		if( skipCounter == 0 ) {
			if( skip ) {
				skipCounter++;
			} else {
				noSkipCounter++;
			}
		} else {
			skipCounter++;
		}
	}

	/**
	 * Process the ELSE annotation.
	 *
	 * @param location The location where the ELSE is encountered.
	 */
	protected void doElse( SourceLocation location ) {
		if( noSkipCounter <= 0 && skipCounter <= 0 ) {
			throw new SourceException( "ELSE without IF encountered", location );
		}
		boolean skip = skipCounter > 0;
		endSkip( location );
		skip( !skip );
	}

	/**
	 * Process the /IF annotation.
	 *
	 * @param location The location where the END IF is encountered.
	 */
	protected void endIf( SourceLocation location ) {
		if( noSkipCounter <= 0 && skipCounter <= 0 ) {
			throw new SourceException( "/IF without IF encountered", location );
		}
		endSkip( location );
	}

	/**
	 * Stop skipping commands. As {@link #skip(boolean)} and {@link #endSkip(SourceLocation)} can be nested, only when
	 * the same number of endSkips are called as the number of skips, the skipping will stop.
	 *
	 * @param location The location where the END SKIP is encountered.
	 */
	protected void endSkip( SourceLocation location ) {
		if( skipCounter > 0 ) {
			skipCounter--;
		} else {
			if( noSkipCounter <= 0 ) {
				throw new SourceException( "/SKIP without SKIP encountered", location );
			}
			noSkipCounter--;
		}
	}

	/**
	 * Are commands to be skipped?
	 *
	 * @return True if commands need to be skipped, false otherwise.
	 */
	public boolean skipping() {
		return skipCounter > 0 || parent != null && parent.skipping();
	}

	/**
	 * Adds a comma separated list of SQLStates to be ignored. See {@link SQLException#getSQLState()}.
	 *
	 * @param ignores A comma separated list of errors to be ignored.
	 */
	protected void pushIgnores( String ignores ) {
		String[] ss = ignores.split( "," );
		for( int i = 0; i < ss.length; i++ ) {
			ss[ i ] = ss[ i ].trim();
		}
		ignoreStack.push( ss );
		refreshIgnores();
	}

	/**
	 * Remove the last added list of ignores.
	 */
	protected void popIgnores() {
		ignoreStack.pop();
		refreshIgnores();
	}

	/**
	 * Synchronize the set of ignores with the queue's contents.
	 */
	protected void refreshIgnores() {
		HashSet<String> ignores = new HashSet<>();
		for( String[] ss : ignoreStack ) {
			ignores.addAll( Arrays.asList( ss ) );
		}
		ignoreSet = ignores;
	}

	/**
	 * Should the given error be ignored?
	 *
	 * @param error SQL Error code.
	 * @return True if the given error should be ignored, false otherwise.
	 */
	public boolean ignoreSQLError( String error ) {
		return ignoreSet.contains( error ) || parent != null && parent.ignoreSQLError( error );
	}

	/**
	 * @return The current scope.
	 */
	public Scope getScope() {
		if( scope == null ) {
			scope = new DefaultScope();
			scope.val( Symbol.apply( "db" ), new ScriptDB( this ) ); // TODO Or var?
		}
		return scope;
	}

	/**
	 * Swap the current scope with the given one.
	 *
	 * @param scope The new scope.
	 * @return The previous scope.
	 */
	public Scope swapScope( Scope scope ) {
		Scope ret = this.scope;
		this.scope = scope;
		return ret;
	}

	/**
	 * Enable or disable JDBC escape processing.
	 *
	 * @param escaping True enables, false disables.
	 */
	public void setJdbcEscaping( boolean escaping ) {
		jdbcEscaping = escaping;
	}

	/**
	 * Is JDBC escape processing enabled?
	 *
	 * @return True if JDBC escape processing is enabled, false otherwise.
	 */
	public boolean isJdbcEscaping() {
		return jdbcEscaping;
	}

	/**
	 * @return True if script expansion is on.
	 */
	public boolean isScriptExpansion() {
		return scriptExpansion;
	}

	/**
	 * Enable or disable script expansion. Script expansion is the execution of scripts inside placeholders in between
	 * the commands and inserting the resulting value.
	 *
	 * @param scriptExpansion True enables script expansion, false disables it.
	 */
	public void setScriptExpansion( boolean scriptExpansion ) {
		this.scriptExpansion = scriptExpansion;
	}

	/**
	 * Set all configured databases.
	 *
	 * @param databases All configured databases.
	 */
	public void setDatabases( DatabaseContext databases ) {
		this.databases = databases;
	}

	/**
	 * Returns all configured databases.
	 *
	 * @return All configured databases.
	 */
	public Collection<Database> getDatabases() {
		return databases.getDatabases();
	}

	/**
	 * Returns the database with the given name.
	 *
	 * @param name The name of the database.
	 * @return The database with the given name.
	 */
	public Database getDatabase( String name ) {
		return databases.getDatabase( name );
	}

	/**
	 * Returns the current database.
	 *
	 * @return The current database.
	 */
	public Database getCurrentDatabase() {
		return currentDatabase;
	}

	/**
	 * Sets the current database.
	 *
	 * @param database The database.
	 */
	public void setCurrentDatabase( Database database ) {
		currentDatabase = database;
	}

	/**
	 * Returns the parent context.
	 *
	 * @return The parent context.
	 */
	public CommandContext getParent() {
		return parent;
	}

	/**
	 * Returns the section nesting level.
	 *
	 * @return The section nesting level.
	 */
	public int getSectionLevel() {
		return sectionLevel;
	}

	/**
	 * Sets the section nesting level.
	 *
	 * @param level The section nesting level.
	 */
	public void setSectionLevel( int level ) {
		sectionLevel = level;
	}

	/**
	 * Ends the current process. This closes all connections to all databases.
	 */
	public void end() {
		for( Database database : getDatabases() ) {
			database.closeConnections();
		}
	}

	/**
	 * @return The currently active commit strategy.
	 */
	public CommitStrategy commitStrategy() {
		return commitStrategy;
	}

	/**
	 * Sets the commit strategy.
	 *
	 * @param strategy The commit strategy.
	 */
	public void setCommitStrategy( CommitStrategy strategy ) {
		commitStrategy = strategy;
	}

	/**
	 * Are we in transient mode?
	 *
	 * @return True if in transient mode, false otherwise.
	 */
	public boolean isTransient() {
		return false;
	}

}
