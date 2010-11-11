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

import java.sql.SQLException;

/**
 * A CommandListener listens to commands from the upgrade or SQL file as they are being processed.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
abstract public class CommandListener
{
	/**
	 * Called when a command from the upgrade file needs to be executed. Commands can be transient or persistent (see
	 * {@link Command#isTransient}). This method should return true if it accepted the command.
	 * 
	 * @param processor The command processor.
	 * @param command The command that needs to be executed.
	 * @return True if it accepted the command.
	 * @throws SQLException When the execution of the command fails with an {@link SQLException}.
	 */
	abstract public boolean execute( CommandProcessor processor, Command command ) throws SQLException;

	/**
	 * Gives this listener a chance to cleanup. For example to kill threads that it started or temporary tables that it
	 * created.
	 */
	public void terminate()
	{
		// Can be overridden by subclasses.
	}
}
