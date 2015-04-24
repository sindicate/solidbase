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


public class SQLExecutionException extends SQLException
{
	protected Command command;
	protected SQLException sqlException;

	public SQLExecutionException( Command command, SQLException e )
	{
		super( e.getCause() );
		this.command = command;
		this.sqlException = e;
	}

	@Override
	public String getMessage()
	{
		return this.sqlException.getMessage() + "\nSQLState: " + this.sqlException.getSQLState() + "\nWhile executing: " + this.command.getCommand();
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
