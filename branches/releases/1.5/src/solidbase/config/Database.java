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

import java.util.ArrayList;
import java.util.List;

import solidbase.core.SystemException;


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
	 * A list of applications contained within the database.
	 */
	protected List< Application > applications = new ArrayList();


	/**
	 * Constructor.
	 * 
	 * @param name The name of the database.
	 * @param description An optional description of the database.
	 * @param driver The driver for the database.
	 * @param url The url of the database.
	 */
	public Database( String name, String description, String driver, String url )
	{
		this.name = name;
		this.description = description;
		this.driver = driver;
		this.url = url;
	}

	/**
	 * Adds an application to the database.
	 * 
	 * @param name The name of the application.
	 * @param description An optional description of the application.
	 * @param userName The user name for the connection.
	 * @param password The password for the connection.
	 * @param patchFile The upgrade file for the application.
	 * @param target The target version to upgrade to.
	 * @return The application object.
	 */
	public Application addApplication( String name, String description, String userName, String password, String patchFile, String target )
	{
		Application application = new Application( name, description, userName, password, patchFile, target );
		this.applications.add( application );
		return application;
	}

	/**
	 * Returns the application with the given name.
	 * 
	 * @param name The name of the application to return.
	 * @return The application with the given name.
	 */
	public Application getApplication( String name )
	{
		for( Application application : this.applications )
			if( application.name.equals( name ) )
				return application;
		throw new SystemException( "Application [" + name + "] not configured for database [" + this.name + "]." );
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
	 * Returns a list of all the applications contained in the database.
	 * 
	 * @return A list of all the applications contained in the database.
	 */
	public List< Application > getApplications()
	{
		return this.applications;
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
