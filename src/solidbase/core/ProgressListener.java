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

import solidstack.io.Resource;

/**
 * Listener adapter.
 *
 * @author R.M. de Bloois
 * @since Apr 14, 2006
 */
@SuppressWarnings( "unused" )
abstract public class ProgressListener
{
	/**
	 * A store for nested messages coming from SECTIONs in the command file.
	 */
	protected String[] messages = new String[ 10 ];


	/**
	 * Returns to column 1 and generates a newline if needed.
	 */
	abstract public void cr();

	/**
	 * Prints a line completely by itself. Generates a carriage return before printing if needed.
	 *
	 * @param message The message to be printed.
	 */
	abstract public void println( String message );
	// TODO Also add a message() which automatically indents one more than the last section

	/**
	 * Resets all state that the listener has.
	 */
	public void reset()
	{
		startSection( 0, null ); // This clears up all section levels.
	}

	/**
	 * An upgrade file is about to be opened.
	 *
	 * @param file The upgrade file that is about to be opened.
	 */
	protected void openingUpgradeFile( Resource file )
	{
		cr();
		println( "Opening file '" + file + "'" );
	}

	/**
	 * An sql file is about to be opened.
	 *
	 * @param sqlFile The sql file that is about to be opened.
	 */
	protected void openingSQLFile( Resource sqlFile )
	{
		cr();
		println( "Opening file '" + sqlFile + "'" );
	}

	/**
	 * An upgrade file is opened.
	 *
	 * @param file The upgrade file that is opened.
	 */
	protected void openedUpgradeFile( UpgradeFile file )
	{
		cr();
		println( "    Encoding is '" + file.getEncoding() + "'" );
	}

	/**
	 * An sql file is opened.
	 *
	 * @param sqlFile The sql file that is opened.
	 */
	protected void openedSQLFile( SQLFile sqlFile )
	{
		cr();
		println( "    Encoding is '" + sqlFile.getEncoding() + "'" );
	}

	/**
	 * The given change set is about to be processed.
	 *
	 * @param segment The segment that is about to be started.
	 */
	protected void upgradeStarting( UpgradeSegment segment )
	{
		// could be implemented in subclass
	}

	/**
	 * A section is started.
	 *
	 * @param level Section level.
	 * @param message Message.
	 */
	protected void startSection( int level, String message )
	{
		this.messages[ level++ ] = message;
		while( level < this.messages.length )
			this.messages[ level++ ] = null; // Deeper sections are reset
	}

	/**
	 * About to execute to given command with the given message for the user.
	 *
	 * @param command The command that is about to be executed.
	 */
	protected void executing( Command command )
	{
		// could be implemented in subclass
	}

	/**
	 * An exception occurred during execution of the given command.
	 *
	 * @param exception The exception that occurred.
	 */
	// TODO Should this be FatalException?
	protected void exception( ProcessException exception )
	{
		// could be implemented in subclass
	}

	/**
	 * A command is successfully executed.
	 */
	protected void executed()
	{
		// could be implemented in subclass
	}

	/**
	 * A change set is completed.
	 */
	protected void upgradeFinished()
	{
		cr();
	}

	/**
	 * The upgrade is completed.
	 */
	protected void upgradeComplete()
	{
		cr();
		println( "Upgrade complete." );
	}

	/**
	 * The upgrade is aborted.
	 */
	protected void upgradeAborted()
	{
		cr();
		println( "Upgrade aborted." );
	}

	/**
	 * No upgrade is needed.
	 */
	protected void noUpgradeNeeded()
	{
		cr();
		println( "No upgrade is needed." );
	}

	/**
	 * The sql execution is completed.
	 */
	protected void sqlExecutionComplete()
	{
		cr();
		println( "Execution complete." );
	}

	/**
	 * The sql execution is aborted.
	 */
	protected void sqlExecutionAborted()
	{
		cr();
		println( "Execution aborted." );
	}

	/**
	 * Request a password for the given user name.
	 *
	 * @param user The user name for which a password needs to be requested.
	 * @return The password that is requested.
	 */
	protected String requestPassword( String user )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * A command is skipped because it has been processed earlier in an upgrade that did not complete.
	 *
	 * @param command The command that is skipped.
	 */
	protected void skipped( Command command )
	{
		// could be implemented in subclass
	}

	/**
	 * A debug message is given.
	 *
	 * @param message The debug message.
	 */
	protected void debug( String message )
	{
		// could be implemented in subclass
	}

	/**
	 * An info message is produced.
	 *
	 * @param message The info message.
	 */
	public void print( String message )
	{
		// could be implemented in subclass
	}
}
