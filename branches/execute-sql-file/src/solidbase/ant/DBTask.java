/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Ant task that has database connection functionality.
 * 
 * @author René M. de Bloois
 */
public class DBTask extends Task
{
	/**
	 * Field to store the configured driver.
	 */
	protected String driver;

	/**
	 * Field to store the configured url.
	 */
	protected String url;

	/**
	 * Field to store the configured user name.
	 */
	protected String username;

	/**
	 * Field to store the configured password.
	 */
	protected String password;

	/**
	 * Field to store the nested collection of secondary connections.
	 */
	protected List< Connection > connections = new ArrayList< Connection >();

	/**
	 * Returns the configured driver.
	 * 
	 * @return The configured driver.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Sets the driver to be configured.
	 * 
	 * @param driver The driver to be configured.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	/**
	 * Returns the configured url.
	 * 
	 * @return The configured url.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Sets the url to be configured.
	 * 
	 * @param url The url to be configured.
	 */
	public void setUrl( String url )
	{
		this.url = url;
	}

	/**
	 * Returns the configured user name.
	 * 
	 * @return The configured user name.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Sets the user name to configure.
	 * 
	 * @param username The user name to configure.
	 */
	public void setUsername( String username )
	{
		this.username = username;
	}

	/**
	 * Returns the configured password.
	 * 
	 * @return The configured password.
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * Sets the password to configure.
	 * 
	 * @param password The password to configure.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}

	/**
	 * Creates a secondary connection.
	 * 
	 * @return The secondary connection created.
	 */
	public Connection createSecondary()
	{
		Connection connection = new Connection();
		this.connections.add( connection );
		return connection;
	}

	/**
	 * Returns all configured secondary connections.
	 * 
	 * @return All configured connections.
	 */
	public List< Connection > getConnections()
	{
		return this.connections;
	}

	/**
	 * Connection object used to configure the Ant Task.
	 * 
	 * @author René M. de Bloois
	 */
	protected class Connection
	{
		/**
		 * The configured name of the secondary connection.
		 */
		protected String name;

		/**
		 * The configured database driver of the secondary connection.
		 */
		protected String driver;

		/**
		 * The configured database url of the secondary connection.
		 */
		protected String url;

		/**
		 * The configured user name of the secondary connection.
		 */
		protected String username;

		/**
		 * The configured password of the secondary connection.
		 */
		protected String password;

		/**
		 * Returns the configured name of the secondary connection.
		 * 
		 * @return The configured name of the secondary connection.
		 */
		public String getName()
		{
			return this.name;
		}

		/**
		 * Sets the name of the secondary connection to configure.
		 * 
		 * @param name The name of the secondary connection to configure.
		 */
		public void setName( String name )
		{
			this.name = name;
		}

		/**
		 * Returns the configured database driver of the secondary connection.
		 * 
		 * @return The configured database driver of the secondary connection.
		 */
		public String getDriver()
		{
			return this.driver;
		}

		/**
		 * Sets the database driver of the secondary connection to configure.
		 * 
		 * @param driver The database driver of the secondary connection to configure.
		 */
		public void setDriver( String driver )
		{
			this.driver = driver;
		}

		/**
		 * Returns the configured database url of the secondary connection.
		 * 
		 * @return The configured database url of the secondary connection.
		 */
		public String getUrl()
		{
			return this.url;
		}

		/**
		 * Sets the database url of the secondary connection to configure.
		 * 
		 * @param url The database url of the secondary connection to configure.
		 */
		public void setUrl( String url )
		{
			this.url = url;
		}

		/**
		 * Returns the configured user name of the secondary connection.
		 * 
		 * @return The configured user name of the secondary connection.
		 */
		public String getUsername()
		{
			return this.username;
		}

		/**
		 * Sets the user name of the secondary connection to configure.
		 * 
		 * @param username The user name of the secondary connection to configure.
		 */
		public void setUsername( String username )
		{
			this.username = username;
		}

		/**
		 * Returns the configured password of the secondary connection.
		 * 
		 * @return The configured password of the secondary connection.
		 */
		public String getPassword()
		{
			return this.password;
		}

		/**
		 * Sets the password of the secondary connection to configure.
		 * 
		 * @param password The password of the secondary connection to configure.
		 */
		public void setPassword( String password )
		{
			this.password = password;
		}
	}

	/**
	 * Validates the configuration of the Ant Task.
	 */
	protected void validate()
	{
		if( this.driver == null )
			throw new BuildException( "The 'driver' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.url == null )
			throw new BuildException( "The 'url' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.username == null )
			throw new BuildException( "The 'user' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.password == null )
			throw new BuildException( "The 'password' attribute is mandatory for the " + getTaskName() + " task" );

		for( Connection connection : this.connections )
		{
			if( connection.getName() == null )
				throw new BuildException( "The 'name' attribute is mandatory for a 'connection' element" );
			if( connection.getUsername() == null )
				throw new BuildException( "The 'user' attribute is mandatory for a 'connection' element" );
			if( connection.getPassword() == null )
				throw new BuildException( "The 'password' attribute is mandatory for a 'connection' element" );
			if( connection.getName().equals( "default" ) )
				throw new BuildException( "The connection name 'default' is reserved" );
		}
	}
}
