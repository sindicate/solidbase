/*--
 * Copyright 2011 René M. de Bloois
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

package solidbase.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import solidbase.core.SystemException;

/**
 * A datasource that gets its connections from the DriverManager.
 *
 * @author René M. de Bloois
 */
public class DriverDataSource implements DataSource
{
	/**
	 * The class name of the driver to be used to access the database.
	 */
	protected String driverClassName;

	/**
	 * The URL of the database.
	 */
	protected String url;

	/**
	 * The default user name to use for this database.
	 */
	protected String username;

	/**
	 * The password belonging to the default user.
	 */
	protected String password;


	/**
	 * Constructor.
	 *
	 * @param driverClassName The database driver class name.
	 * @param url The database URL.
	 * @param username The default user name.
	 * @param password The password for the default user.
	 */
	public DriverDataSource( String driverClassName, String url, String username, String password )
	{
		Assert.notNull( driverClassName );
		Assert.notNull( url );

		this.driverClassName = driverClassName;
		this.url = url;
		this.username = username;
		this.password = password;

		try
		{
			Class.forName( driverClassName );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Returns a new connection using the default user.
	 */
	public Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection( this.url, this.username, this.password );
	}

	/**
	 * Returns a new connection using the given user name and password.
	 *
	 * @param username The user name to connect with.
	 * @param password The password of the user.
	 */
	public Connection getConnection( String username, String password ) throws SQLException
	{
		return DriverManager.getConnection( this.url, username, password );
	}

	public PrintWriter getLogWriter() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setLogWriter( PrintWriter out ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setLoginTimeout( int seconds ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getLoginTimeout() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public < T > T unwrap( Class< T > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isWrapperFor( Class< ? > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		throw new UnsupportedOperationException();
	}
}
