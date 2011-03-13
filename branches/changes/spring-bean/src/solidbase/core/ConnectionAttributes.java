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

package solidbase.core;

import javax.sql.DataSource;

/**
 * A connection used by the {@link Runner}.
 *
 * @author René M. de Bloois
 */
public class ConnectionAttributes
{
	/**
	 * The name of the connection.
	 */
	protected String name;

	/**
	 * The driver class name of the connection.
	 */
	protected String driver;

	/**
	 * The URL of the connection.
	 */
	protected String url;

	/**
	 * The data source for the connection.
	 */
	protected DataSource datasource;

	/**
	 * The user name of the connection.
	 */
	protected String username;

	/**
	 * The password of the connection.
	 */
	protected String password;


	/**
	 * Constructor.
	 */
	public ConnectionAttributes()
	{
		// Default constructor
	}

	/**
	 * Constructor.
	 *
	 * @param name The name of the connection.
	 * @param driver The driver class name of the connection.
	 * @param url The URL of the connection.
	 * @param username The user name of the connection.
	 * @param password The password of the connection.
	 */
	public ConnectionAttributes( String name, String driver, String url, String username, String password )
	{
		this.name = name;
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
	}

	/**
	 * Constructor.
	 *
	 * @param name The name of the connection.
	 * @param dataSource The data source for the connection.
	 * @param username The user name of the connection.
	 * @param password The password of the connection.
	 */
	public ConnectionAttributes( String name, DataSource dataSource, String username, String password )
	{
		this.name = name;
		this.datasource = dataSource;
		this.username = username;
		this.password = password;
	}

	/**
	 * Returns the name of the connection.
	 *
	 * @return The name of the connection.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Sets the name of the connection.
	 *
	 * @param name The name of the connection.
	 */
	public void setName( String name )
	{
		this.name = name;
	}

	/**
	 * Returns the driver class name of the connection.
	 *
	 * @return The driver class name of the connection.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Sets the driver class name of the connection.
	 *
	 * @param driver The driver class name of the connection.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	/**
	 * Returns the URL of the connection.
	 *
	 * @return The URL of the connection.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Sets the URL of the connection.
	 *
	 * @param url The URL of the connection.
	 */
	public void setUrl( String url )
	{
		this.url = url;
	}

	/**
	 * Returns the data source.
	 *
	 * @return The data source.
	 */
	public DataSource getDatasource()
	{
		return this.datasource;
	}

	/**
	 * Sets the data source.
	 *
	 * @param datasource the data source.
	 */
	public void setDatasource( DataSource datasource )
	{
		this.datasource = datasource;
	}

	/**
	 * Returns the user name of the connection.
	 *
	 * @return The user name of the connection.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Sets the user name of the connection.
	 *
	 * @param username The user name of the connection.
	 */
	public void setUsername( String username )
	{
		this.username = username;
	}

	/**
	 * Returns the password of the connection.
	 *
	 * @return The password of the connection.
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * Sets the password of the connection.
	 *
	 * @param password The password of the connection.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}
}
