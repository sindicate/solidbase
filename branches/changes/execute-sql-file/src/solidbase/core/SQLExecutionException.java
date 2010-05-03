/*--
 * Copyright 2009 René M. de Bloois
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
import java.util.Iterator;


/**
 * This subclass of {@link SQLException} combines an SQLException together with the command that caused the exception.
 * 
 * @author René M. de Bloois
 */
public class SQLExecutionException extends SQLException
{
	/**
	 * The command that caused the {@link SQLException}.
	 */
	protected Command command;

	/**
	 * The {@link SQLException}.
	 */
	protected SQLException sqlException;


	/**
	 * Constructor.
	 * 
	 * @param command The command that caused the {@link SQLException}.
	 * @param e The {@link SQLException}.
	 */
	public SQLExecutionException( Command command, SQLException e )
	{
		super( e.getCause() );
		this.command = command;
		this.sqlException = e;
	}

	@Override
	public String getMessage()
	{
		String command = this.command.getCommand();
		if( command.length() > 1000 )
			command = command.substring( 0, 1000 ) + "...";
		return this.sqlException.getMessage() + "\nSQLState: " + this.sqlException.getSQLState() + "\nWhile executing line " + this.command.getLineNumber() + ": " + command;
	}

	@Override
	public int getErrorCode()
	{
		return this.sqlException.getErrorCode();
	}

	@Override
	public SQLException getNextException()
	{
		return this.sqlException.getNextException();
	}

	@Override
	public String getSQLState()
	{
		return this.sqlException.getSQLState();
	}

	@Override
	public Iterator< Throwable > iterator()
	{
		return this.sqlException.iterator();
	}

	@Override
	public void setNextException( SQLException ex )
	{
		this.sqlException.setNextException( ex );
	}
}
