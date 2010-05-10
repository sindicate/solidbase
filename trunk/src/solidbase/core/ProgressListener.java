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
public class ProgressListener
{
	/**
	 * An upgrade file is about to be opened.
	 * 
	 * @param patchFile The upgrade file that is about to be opened.
	 */
	protected void openingPatchFile( File patchFile )
	{
		// could be implemented in subclass
	}

	/**
	 * An upgrade file is about to be opened.
	 * 
	 * @param patchFile The upgrade file that is about to be opened.
	 */
	protected void openingPatchFile( URL patchFile )
	{
		// could be implemented in subclass
	}

	/**
	 * An sql file is about to be opened.
	 * 
	 * @param sqlFile The sql file that is about to be opened.
	 */
	protected void openingSQLFile( File sqlFile )
	{
		// could be implemented in subclass
	}

	/**
	 * An sql file is about to be opened.
	 * 
	 * @param sqlFile The sql file that is about to be opened.
	 */
	protected void openingSQLFile( URL sqlFile )
	{
		// could be implemented in subclass
	}

	/**
	 * An upgrade file is opened.
	 * 
	 * @param patchFile The upgrade file that is opened.
	 */
	protected void openedPatchFile( PatchFile patchFile )
	{
		// could be implemented in subclass
	}

	/**
	 * An sql file is opened.
	 * 
	 * @param sqlFile The sql file that is opened.
	 */
	protected void openedSQLFile( SQLFile sqlFile )
	{
		// could be implemented in subclass
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
	 * An exception occured during execution of the given command.
	 * 
	 * @param command The command during which an exception occured.
	 */
	protected void exception( Command command )
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
		// could be implemented in subclass
	}

	/**
	 * The upgrade is completed.
	 */
	protected void upgradeComplete()
	{
		// could be implemented in subclass
	}

	/**
	 * The sql execution is completed.
	 */
	protected void sqlExecutionComplete()
	{
		// could be implemented in subclass
	}

	/**
	 * Request a password for the given user name.
	 * 
	 * @param user The user name for which a password needs to be requested.
	 * @return The password that is requested.
	 */
	protected String requestPassword( String user )
	{
		// could be implemented in subclass
		return null;
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
