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

package ronnie.dbpatcher.core;

import java.sql.SQLException;

/**
 * A CommandListener listens to commands in the patch file as they are being executed.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
abstract public class CommandListener
{
	/**
	 * Called when a command from the patch file needs to be executed. Commands can be repeatable or non-repeatable commands (see {@link Command#isRepeatable()}). This method should
	 * return true when it wants to indicate to the patchtool that it should stop processing this command.
	 *
	 * @param command The command that needs to be executed.
	 * @return true to signal the patchtool to stop processing the command, false otherwise.
	 * @throws SQLException
	 */
	abstract protected boolean execute( Database database, Command command ) throws SQLException;

	/**
	 * Gives the listener a chance to cleanup after itself. For example to kill threads that it started or temporary tables that it created.
	 *
	 */
	protected void terminate()
	{
		//
	}
}
