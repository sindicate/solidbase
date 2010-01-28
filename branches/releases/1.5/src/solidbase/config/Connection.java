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

package solidbase.config;

/**
 * A configured connection to a database.
 * 
 * @author René M. de Bloois
 */
public class Connection
{
	/**
	 * The name of the connection.
	 */
	protected String name;

	/**
	 * The driver name of the database.
	 */
	protected String driver;

	/**
	 * The url of the database.
	 */
	protected String url;

	/**
	 * The user name for the connection to the database.
	 */
	protected String user;

	/**
	 * The password for the connection to the database.
	 */
	protected String password;

	// TODO Rename user to username everywhere

	/**
	 * Constructor.
	 * 
	 * @param name The name of the connection.
	 * @param driver The driver name of the database.
	 * @param url The url of the database.
	 * @param user The user name for the database connection.
	 * @param password The password for the database connection.
	 */
	public Connection( String name, String driver, String url, String user, String password )
	{
		this.name = name;
		this.driver = driver;
		this.url = url;
		this.user = user;
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
	 * Return the driver for the database.
	 * 
	 * @return The driver for the database.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Returns the url of the database.
	 * 
	 * @return The url of the database.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Returns the user name for the database connection.
	 * 
	 * @return The user name for the database connection.
	 */
	public String getUser()
	{
		return this.user;
	}

	/**
	 * Returns the password for the database connection.
	 * 
	 * @return The password for the database connection.
	 */
	public String getPassword()
	{
		return this.password;
	}
}
