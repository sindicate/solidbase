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
	 * The command reader.
	 */
	protected SQLSource sqlSource;

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
	 * Sets the source for the SQL.
	 * 
	 * @param source the source for the SQL.
	 */
	public void setSQLSource( SQLSource source )
	{
		this.sqlSource = source;
	}

	/**
	 * Execute the SQL file.
	 * 
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void process() throws SQLExecutionException
	{
		reset(); // TODO This is not unit-tested yet.

		Command command = this.sqlSource.readCommand();
		while( command != null )
		{
			if( command.isTransient() || this.skipCounter == 0 )
				executeWithListeners( command );
			else
				this.progress.skipped( command );
			command = this.sqlSource.readCommand();
		}
	}

	@Override
	protected void startSection( int level, String message )
	{
		super.startSection( level > 0 ? level - 1 : level, message );
	}

	@Override
	public void end()
	{
		super.end();
		this.sqlSource.close();
		this.progress.sqlExecutionComplete();
	}

	@Override
	protected void setDelimiters( Delimiter[] delimiters )
	{
		this.sqlSource.setDelimiters( delimiters );
	}
}
