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
 * A configured database.
 * 
 * @author René M. de Bloois
 */
public class Database
{
	/**
	 * The name of the database.
	 */
	protected String name;

	/**
	 * The driver for the database.
	 */
	protected String driver;

	/**
	 * The url of the database.
	 */
	protected String url;

	/**
	 * The username for the connection to the database.
	 */
	protected String userName;

	/**
	 * The password for the connection to the database.
	 */
	protected String password;


	/**
	 * Constructor.
	 * 
	 * @param name The name of the database.
	 */
	public Database( String name )
	{
		this.name = name;
	}

	/**
	 * Constructor.
	 * 
	 * @param name The name of the database.
	 * @param driver The driver for the database.
	 * @param url The url of the database.
	 * @param userName The user name for the connection to the database.
	 * @param password The optional password for the connection to the database.
	 */
	public Database( String name, String driver, String url, String userName, String password )
	{
		this.name = name;
		this.driver = driver;
		this.url = url;
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Sets the driver for the database.
	 * 
	 * @param driver The driver for the database.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	/**
	 * Sets the url of the database.
	 * 
	 * @param url The url of the database.
	 */
	public void setUrl( String url )
	{
		this.url = url;
	}

	/**
	 * Sets the user name for the connection to the database.
	 * 
	 * @param userName The user name for the connection to the database.
	 */
	public void setUserName( String userName )
	{
		this.userName = userName;
	}

	/**
	 * Sets the password for the connection to the database.
	 * 
	 * @param password The password for the connection to the database.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}

	/**
	 * Returns the name of the database.
	 * 
	 * @return The name of the database.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the driver for this database.
	 * 
	 * @return The driver for this database.
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
	 * Returns the user name for the connection to the database.
	 * 
	 * @return The user name for the connection to the database.
	 */
	public String getUserName()
	{
		return this.userName;
	}

	/**
	 * Returns the user name for the connection to the database.
	 * 
	 * @return The user name for the connection to the database.
	 */
	public String getPassword()
	{
		return this.password;
	}
}
