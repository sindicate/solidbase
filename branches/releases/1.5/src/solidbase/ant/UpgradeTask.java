/*--
 * Copyright 2009 Ren� M. de Bloois
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import solidbase.Main;
import solidbase.config.Configuration;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;


/**
 * The Upgrade Ant Task.
 * 
 * @author Ren� M. de Bloois
 */
public class UpgradeTask extends Task
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
	 * Field to store the configured upgrade file.
	 */
	protected String upgradefile;

	/**
	 * Field to store the configured target.
	 */
	protected String target;

	/**
	 * Field to store the configured downgrade allowed option.
	 */
	protected boolean downgradeallowed;

	/**
	 * Field to store the nested collection of secondary connections.
	 */
	protected List< Connection > connections = new ArrayList< Connection >();

//	protected Path classpath;
//
//	public Path createClasspath()
//	{
//		if( this.classpath == null )
//			this.classpath = new Path( getProject() );
//		return this.classpath.createPath();
//	}
//
//	public Path getClasspath()
//	{
//		return this.classpath;
//	}
//
//	public void setClasspath( Path classpath )
//	{
//		if( this.classpath == null )
//			this.classpath = new Path( getProject() );
//		this.classpath.append( classpath );
//	}
//
//	public void setClasspathref( Reference reference )
//	{
//		if( this.classpath == null )
//			this.classpath = new Path( getProject() );
//		this.classpath.createPath().setRefid( reference );
//	}

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

	public void setUrl( String url )
	{
		this.url = url;
	}

	public String getUsername()
	{
		return this.username;
	}

	public void setUsername( String username )
	{
		this.username = username;
	}

	@Deprecated
	public void setUser( String username )
	{
		this.username = username;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void setPassword( String password )
	{
		this.password = password;
	}

	public String getUpgradefile()
	{
		return this.upgradefile;
	}

	public void setUpgradefile( String upgradefile )
	{
		this.upgradefile = upgradefile;
	}

	public String getTarget()
	{
		return this.target;
	}

	public void setTarget( String target )
	{
		this.target = target;
	}

	public boolean isDowngradeallowed()
	{
		return this.downgradeallowed;
	}

	public void setDowngradeallowed( boolean downgradeallowed )
	{
		this.downgradeallowed = downgradeallowed;
	}

	public Connection createSecondary()
	{
		Connection connection = new Connection();
		this.connections.add( connection );
		return connection;
	}

	public List< Connection > getConnections()
	{
		return this.connections;
	}

	protected class Connection
	{
		protected String name;
		protected String driver;
		protected String url;
		protected String user;
		protected String password;

		public String getName()
		{
			return this.name;
		}

		public void setName( String name )
		{
			this.name = name;
		}

		public String getDriver()
		{
			return this.driver;
		}

		public void setDriver( String driver )
		{
			this.driver = driver;
		}

		public String getUrl()
		{
			return this.url;
		}

		public void setUrl( String url )
		{
			this.url = url;
		}

		public String getUser()
		{
			return this.user;
		}

		public void setUser( String user )
		{
			this.user = user;
		}

		public String getPassword()
		{
			return this.password;
		}

		public void setPassword( String password )
		{
			this.password = password;
		}
	}


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
		if( this.upgradefile == null )
			throw new BuildException( "The 'upgradefile' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.target == null )
			throw new BuildException( "The 'target' attribute is mandatory for the " + getTaskName() + " task" );

		for( Connection connection : this.connections )
		{
			if( connection.getName() == null )
				throw new BuildException( "The 'name' attribute is mandatory for a 'connection' element" );
			if( connection.getUser() == null )
				throw new BuildException( "The 'user' attribute is mandatory for a 'connection' element" );
			if( connection.getPassword() == null )
				throw new BuildException( "The 'password' attribute is mandatory for a 'connection' element" );
			if( connection.getName().equals( "default" ) )
				throw new BuildException( "The connection name 'default' is reserved" );
		}
	}


	@Override
	public void execute()
	{
		validate();

		Project project = getProject();
		Progress progress = new Progress( project, this );
		Configuration configuration = new Configuration( progress );

		progress.info( "SolidBase v" + configuration.getVersion() );
		// TODO Ant messes up the encoding, try add the � again
		progress.info( "(C) 2006-2010 Rene M. de Bloois" );
		progress.info( "" );

		Patcher patcher = new Patcher( progress, new Database( this.driver, this.url, this.username, this.password, progress ) );
		try
		{
			for( Connection connection : this.connections )
				patcher.addConnection( new solidbase.config.Connection( connection.getName(), connection.getDriver(), connection.getUrl(), connection.getUser(), connection.getPassword() ) );

			progress.info( "Connecting to database..." );

			progress.info( Main.getCurrentVersion( patcher ) );

			patcher.openPatchFile( project.getBaseDir(), this.upgradefile );
			try
			{
				if( this.target != null )
					patcher.patch( this.target, this.downgradeallowed ); // TODO Print this target
				else
					throw new UnsupportedOperationException();
				progress.info( "" );
				progress.info( Main.getCurrentVersion( patcher ) );
			}
			finally
			{
				patcher.closePatchFile();
			}
		}
		catch( SQLExecutionException e )
		{
			throw new BuildException( e.getMessage() );
		}
		catch( FatalException e )
		{
			throw new BuildException( e.getMessage() );
		}
		finally
		{
			patcher.end();
		}
	}
}
