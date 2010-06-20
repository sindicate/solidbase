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
 * This class is the coordinator. It reads commands from the {@link SQLFile}. It calls the {@link CommandListener}s,
 * calls the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 * 
 * @author René M. de Bloois
 * @since May 2010
 */
public class SQLProcessor extends CommandProcessor
{
	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 */
	public SQLProcessor( ProgressListener listener )
	{
		super( listener );
	}

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public SQLProcessor( ProgressListener listener, Database database )
	{
		super( listener, database );
	}

	/**
	 * Execute the SQL file.
	 * 
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void execute() throws SQLExecutionException
	{
		Command command = this.commandSource.readCommand();
		while( command != null )
		{
			executeWithListeners( command );
			command = this.commandSource.readCommand();
		}
		this.progress.sqlExecutionComplete();
	}

	@Override
	protected void startSection( int level, String message )
	{
		super.startSection( level > 0 ? level - 1 : level, message );
	}
}
