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

package solidbase.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import solidbase.Version;
import solidbase.core.ConnectionAttributes;
import solidbase.core.Database;
import solidbase.core.DatabaseContext;
import solidbase.core.Factory;
import solidbase.core.SystemException;
import solidbase.core.UpgradeProcessor;
import solidbase.util.DriverDataSource;
import solidstack.io.MemoryResource;
import solidstack.io.URIResource;

/**
 * An upgrade Spring bean.
 *
 * @author René M. de Bloois
 */
public class UpgradeBean
{
	/**
	 * The database driver class. Gets overruled by datasource.
	 */
	private String driver;

	/**
	 * The URL of the database. Gets overruled by datasource.
	 */
	private String url;

	/**
	 * The datasource. Overrules driver and URL.
	 */
	private DataSource datasource;

	/**
	 * The user name to use for connecting to the database.
	 */
	private String username;

	/**
	 * Password for the user.
	 */
	private String password;

	/**
	 * The configured upgrade file.
	 */
	private Resource upgradefile;

	/**
	 * The configured target.
	 */
	private String target;

	/**
	 * The configured downgrade allowed option.
	 */
	protected boolean downgradeallowed;

	/**
	 * The secondary connections.
	 */
	private List< ConnectionAttributes > secondary = new ArrayList< ConnectionAttributes >();

	/**
	 * Returns the database driver class name.
	 *
	 * @return The database driver class name.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Sets the database driver class name.
	 *
	 * @param driver The database driver class name.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	/**
	 * Returns the database URL.
	 *
	 * @return The database URL.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Sets the database URL.
	 *
	 * @param url The database URL.
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
	 * @param datasource The data source.
	 */
	public void setDatasource( DataSource datasource )
	{
		this.datasource = datasource;
	}

	/**
	 * Returns the user name that is used to connect to the database.
	 *
	 * @return The user name.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Sets the user name to use to connect to the database.
	 *
	 * @param username The user name.
	 */
	public void setUsername( String username )
	{
		this.username = username;
	}

	/**
	 * Returns the password for the user.
	 *
	 * @return The password.
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * Sets the password for the user.
	 *
	 * @param password The password.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}

	/**
	 * Returns the upgrade file.
	 *
	 * @return The upgrade file.
	 */
	public Resource getUpgradefile()
	{
		return this.upgradefile;
	}

	/**
	 * Sets the upgrade file.
	 *
	 * @param upgradefile The upgrade file.
	 */
	public void setUpgradefile( Resource upgradefile )
	{
		this.upgradefile = upgradefile;
	}

	/**
	 * Returns the target to upgrade the database to.
	 *
	 * @return The target.
	 */
	public String getTarget()
	{
		return this.target;
	}

	/**
	 * Sets the target to upgrade the database to.
	 *
	 * @param target The target.
	 */
	public void setTarget( String target )
	{
		this.target = target;
	}

	/**
	 * Returns the secondary connections.
	 *
	 * @return The secondary connections.
	 */
	public List< ConnectionAttributes > getSecondary()
	{
		return this.secondary;
	}

	/**
	 * Sets the secondary connections.
	 *
	 * @param secondary The secondary connections.
	 */
	public void setSecondary( List< ConnectionAttributes > secondary )
	{
		this.secondary = secondary;
	}

	/**
	 * Validates the configuration of the upgrade bean.
	 */
	protected void validate()
	{
		if( this.datasource == null )
		{
			Assert.hasText( this.driver, "Missing 'datasource' or 'driver' for " + getClass().getName() );
			Assert.hasText( this.url, "Missing 'datasource' or 'url' for " + getClass().getName() );
			Assert.notNull( this.username, "Missing 'username' for " + getClass().getName() );
			Assert.notNull( this.password, "Missing 'password' for " + getClass().getName() );
		}

		for( ConnectionAttributes connection : this.secondary )
			if( connection.getDatasource() == null )
			{
				Assert.hasText( connection.getName(), "Missing 'name' for " + connection.getClass().getName() );
				Assert.isTrue( !connection.getName().equals( "default" ), "The connection name 'default' is reserved" );
				Assert.notNull( connection.getUsername(), "Missing 'username' for " + connection.getClass().getName() );
				Assert.notNull( connection.getPassword(), "Missing 'password' for " + connection.getClass().getName() );
			}
	}

	/**
	 * Upgrades the database.
	 */
	public void upgrade()
	{
		validate();

		// TODO Use the Runner

		ProgressLogger progress = new ProgressLogger();

		String info = Version.getInfo();
		progress.println( info );
		progress.println( "" );

		DatabaseContext databases = new DatabaseContext();

		DataSource dataSource = this.datasource;
		if( dataSource == null )
			dataSource = new DriverDataSource( this.driver, this.url, this.username, this.password );
		Database database = new Database( "default", dataSource, this.username, this.password, progress );
		databases.addDatabase( database );

		for( ConnectionAttributes secondary : this.secondary )
		{
			DataSource secondaryDataSource = secondary.getDatasource();
			if( secondaryDataSource == null )
				if( secondary.getDriver() != null )
					secondaryDataSource = new DriverDataSource( secondary.getDriver(), secondary.getUrl(), secondary.getUsername(), secondary.getPassword() );
				else
					secondaryDataSource = dataSource;
			databases.addDatabase( new Database( secondary.getName(), secondaryDataSource, secondary.getUsername(), secondary.getPassword(), progress ) );
		}

		UpgradeProcessor processor = new UpgradeProcessor( progress );
		processor.setDatabases( databases );

		try
		{
			solidstack.io.Resource resource;
			if( this.upgradefile instanceof ByteArrayResource )
				resource = new MemoryResource( ( (ByteArrayResource)this.upgradefile ).getByteArray() );
			else if( this.upgradefile.isOpen() )
				// Spring resource isOpen means that the resource cannot be reopened. Thats why we read it into memory.
				resource = new MemoryResource( this.upgradefile.getInputStream() );
			else
				resource = new URIResource( this.upgradefile.getURI() );
			processor.setUpgradeFile( Factory.openUpgradeFile( resource, progress ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		try
		{
			processor.init();
			progress.println( "Connecting to database..." );
			progress.println( processor.getVersionStatement() );
			processor.upgrade( this.target, false );
			progress.println( "" );
			progress.println( processor.getVersionStatement() );
		}
		finally
		{
			processor.end();
		}
	}
}
