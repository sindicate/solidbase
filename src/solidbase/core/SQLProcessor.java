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

import java.sql.SQLException;

import solidbase.util.LineReader;
import solidbase.util.Resource;


/**
 * This class is the coordinator. It reads commands from the {@link SQLFile}. It calls the {@link CommandListener}s,
 * calls the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 *
 * @author Ren� M. de Bloois
 * @since May 2010
 */
public class SQLProcessor extends CommandProcessor
{
	/**
	 * The SQL execution context.
	 */
	protected SQLContext sqlContext;

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
	 * Sets the SQL execution context.
	 *
	 * @param context The SQL execution context.
	 */
	public void setContext( SQLContext context )
	{
		this.context = context;
		this.sqlContext = context;
	}

	/**
	 * Execute the SQL file.
	 *
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void process() throws SQLExecutionException
	{
		this.context.setCurrentDatabase( getDefaultDatabase() );
		this.context.getCurrentDatabase().resetUser();

		Command command = this.sqlContext.getSource().readCommand();
		while( command != null )
		{
			if( this.context.skipping() && command.isPersistent() )
				this.progress.skipped( command );
			else
				executeWithListeners( command ); // TODO What if exception is ignored, how do we call progress then?
			command = this.sqlContext.getSource().readCommand();
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
		for( Database database : this.context.getDatabases() )
			database.closeConnections();
		this.sqlContext.getSource().close();
		this.progress.sqlExecutionComplete(); // TODO Why is this different from UpgradeProcessor.end()?
	}

	@Override
	protected void setDelimiters( Delimiter[] delimiters )
	{
		this.sqlContext.getSource().setDelimiters( delimiters );
	}

	@Override
	public LineReader getReader()
	{
		return this.sqlContext.getSource().reader;
	}

	@Override
	public Resource getResource()
	{
		return this.sqlContext.getSource().getResource();
	}
}
