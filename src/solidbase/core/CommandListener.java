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
public interface CommandListener
{
	/**
	 * Called when a command from the upgrade file needs to be executed. Commands can be transient or persistent (see
	 * {@link Command#isTransient}). This method should return true if it accepted the command.
	 *
	 * Problems caused by the user (command syntax, configuration mistakes or problems in data files) should be wrapped
	 * in a {@link FatalException}.
	 *
	 * {@link SQLException}s should be wrapped in a {@link SQLExecutionException}. You can choose to give it the
	 * original command that triggered this listener or the actual SQL being sent to the database. You may choose to let
	 * the {@link SQLException} pass, then SolidBase will wrap it for you.
	 *
	 * @param processor The command processor.
	 * @param command The command that needs to be executed.
	 * @param skip The command needs to be skipped.
	 * @return True if it accepted the command.
	 * @throws SQLException Whenever an unhandled {@link SQLException} is thrown.
	 */
	boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException;

	/**
	 * Gives this listener a chance to cleanup. For example to kill threads that it started or temporary tables that it
	 * created.
	 */
	void terminate();
}
