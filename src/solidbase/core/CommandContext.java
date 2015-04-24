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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import solidstack.io.SourceLocation;



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

	/**
	 * Variables. Null instead of empty.
	 */
	private Map< String, String > variables;


	/**
	 * Constructor.
	 */
	public CommandContext()
	{
		this.jdbcEscaping = false;
		this.sectionLevel = 0;
		this.variables = null;
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
		if( parent.variables != null )
			this.variables = new HashMap< String, String >( parent.variables );

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
			throw new CommandFileException( "ELSE without IF encountered", location );
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
			throw new CommandFileException( "/IF without IF encountered", location );
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
				throw new CommandFileException( "/SKIP without SKIP encountered", location );
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

	/**
	 * Sets the specified variable.
	 *
	 * @param name The name of the variable.
	 * @param value The value to store into the variable.
	 */
	public void setVariable( String name, Object value )
	{
		if( this.variables == null )
			this.variables = new HashMap< String, String >();
		this.variables.put( name.toUpperCase(), value == null ? null : value.toString() );
	}

	/**
	 * Are any variables defined?
	 *
	 * @return True if variables are defined, false otherwise.
	 */
	public boolean hasVariables()
	{
		return this.variables != null || this.parent != null && this.parent.hasVariables();
	}

	/**
	 * Is the variable with the given name defined?
	 *
	 * @param name The name of the variable.
	 * @return True if the variable is defined, false otherwise.
	 */
	public boolean hasVariable( String name )
	{
		return this.variables != null && this.variables.containsKey( name ) || this.parent != null && this.parent.hasVariable( name );
	}

	/**
	 * Return the value of the variable with the given name.
	 *
	 * @param name The name of the variable.
	 * @return The value of the variable with the given name.
	 */
	public String getVariableValue( String name )
	{
		if( this.variables != null && this.variables.containsKey( name ) )
			return this.variables.get( name );
		if( this.parent == null )
			return null;
		return this.parent.getVariableValue( name );
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
	public boolean getJdbcEscaping()
	{
		return this.jdbcEscaping;
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
}
