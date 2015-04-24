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
import java.net.URL;

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
	 * @param patchFile The upgrade file that is about to be opened.
	 */
	protected void openingPatchFile( File patchFile )
	{
		cr();
		println( "Opening file '" + patchFile + "'" );
	}

	/**
	 * An upgrade file is about to be opened.
	 * 
	 * @param patchFile The upgrade file that is about to be opened.
	 */
	protected void openingPatchFile( URL patchFile )
	{
		cr();
		println( "Opening file '" + patchFile + "'" );
	}

	/**
	 * An sql file is about to be opened.
	 * 
	 * @param sqlFile The sql file that is about to be opened.
	 */
	protected void openingSQLFile( File sqlFile )
	{
		cr();
		println( "Opening file '" + sqlFile + "'" );
	}

	/**
	 * An sql file is about to be opened.
	 * 
	 * @param sqlFile The sql file that is about to be opened.
	 */
	protected void openingSQLFile( URL sqlFile )
	{
		cr();
		println( "Opening file '" + sqlFile + "'" );
	}

	/**
	 * An upgrade file is opened.
	 * 
	 * @param patchFile The upgrade file that is opened.
	 */
	protected void openedPatchFile( PatchFile patchFile )
	{
		cr();
		println( "    Encoding is '" + patchFile.getEncoding() + "'" );
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
	 * @param patch The change set that is about to be started.
	 */
	protected void patchStarting( Patch patch )
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
	 * @param message The message that is set.
	 */
	protected void executing( Command command, String message )
	{
		// could be implemented in subclass
	}

	/**
	 * An exception occurred during execution of the given command.
	 * 
	 * @param exception The exception that occurred.
	 */
	// TODO Should this be FatalException?
	protected void exception( SQLExecutionException exception )
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
	protected void patchFinished()
	{
		cr();
	}

	/**
	 * The upgrade is completed.
	 */
	protected void upgradeComplete()
	{
		cr();
		println( "The database is upgraded." );
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
