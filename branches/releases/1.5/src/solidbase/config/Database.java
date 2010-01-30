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
	 * A description of the database.
	 */
	protected String description;

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
	 * The upgrade file.
	 */
	protected String upgradeFile;


	/**
	 * Constructor.
	 * 
	 * @param name The name of the database.
	 * @param description An optional description of the database.
	 * @param driver The driver for the database.
	 * @param url The url of the database.
	 * @param userName The user name for the connection to the database.
	 * @param password The optional password for the connection to the database.
	 * @param upgradeFile The upgrade file.
	 */
	public Database( String name, String description, String driver, String url, String userName, String password, String upgradeFile )
	{
		this.name = name;
		this.description = description;
		this.driver = driver;
		this.url = url;
		this.userName = userName;
		this.password = password;
		this.upgradeFile = upgradeFile;
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
	 * Returns the description of the database.
	 * 
	 * @return The description of the database.
	 */
	public String getDescription()
	{
		return this.description;
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

	/**
	 * Returns the upgrade file.
	 * 
	 * @return The upgrade file.
	 */
	public String getUpgradeFile()
	{
		return this.upgradeFile;
	}

	/**
	 * A comparator to sort databases by name.
	 * 
	 * @author René M. de Bloois
	 */
	static public class Comparator implements java.util.Comparator< Database >
	{
		public int compare( Database database1, Database database2 )
		{
			return database1.name.compareTo( database2.name );
		}
	}
}
