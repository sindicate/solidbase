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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import solidbase.core.Assert;
import solidbase.core.SystemException;


/**
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static private final String DBPATCHER_PROPERTIES = "solidbase.properties";
	static private final String DBPATCHER_DEFAULT_PROPERTIES = "solidbase-default.properties";
	static private final String DBPATCHER_VERSION_PROPERTIES = "version.properties";

	protected String version;
	protected boolean fromAnt;
	protected Properties properties;

	// Version 2 configuration
	protected List< String > driverJars;
	protected List< Database > databases;

	// Version 1 configuration
	protected int configVersion;
	protected String dbUrl;
	protected String dbDriver;
	protected String dbDriverJar;
	protected String userName;
	protected String passWord;
	protected String target;
	protected String patchFile;

	// Needed for testing
	protected File getPropertiesFile()
	{
		return new File( DBPATCHER_PROPERTIES );
	}

	// Used from the UpgradeTask
	public Configuration( ConfigListener progress )
	{
		// Checks

		// Load the version properties

		URL url = Configuration.class.getResource( DBPATCHER_VERSION_PROPERTIES );
		if( url == null )
			throw new SystemException( "File not found: " + DBPATCHER_VERSION_PROPERTIES );

		try
		{
			Properties versionProperties = new Properties();
			versionProperties.load( url.openStream() );

			this.version = versionProperties.getProperty( "module.version" );

			Assert.isTrue( this.version != null, "module.version not found in version.properties" );

			// Load the default properties

			url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
				throw new SystemException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

			progress.readingPropertyFile( url.toString() );
			this.properties = new Properties();
			this.properties.load( url.openStream() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	// TODO The automatic target should also be added to the properties file
	public Configuration( ConfigListener progress, int pass, String optionDriver, String optionUrl, String optionUserName, String optionPassWord, String optionTarget, String optionPatchFile )
	{
		// Checks

		if( optionDriver != null )
			Assert.isTrue( optionUrl != null && optionUserName != null );
		else
			Assert.isTrue( optionUrl == null && optionUserName == null );

		// Load the version properties

		URL url = Configuration.class.getResource( DBPATCHER_VERSION_PROPERTIES );
		if( url == null )
			throw new SystemException( "File not found: " + DBPATCHER_VERSION_PROPERTIES );

		try
		{
			Properties versionProperties = new Properties();
			versionProperties.load( url.openStream() );

			this.version = versionProperties.getProperty( "module.version" );

			Assert.isTrue( this.version != null, "module.version not found in version.properties" );

			// Process the commandline options

			if( optionDriver != null )
			{
				this.configVersion = 1;
				this.dbDriver = optionDriver;
				this.dbUrl = optionUrl;
				this.userName = optionUserName;
				this.passWord = optionPassWord;
				this.target = optionTarget;
				this.patchFile = optionPatchFile;

				return; // No need to read the properties
			}

			// Load the default properties

			url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
				throw new SystemException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

			progress.readingPropertyFile( url.toString() );
			Properties defaultProperties = new Properties();
			defaultProperties.load( url.openStream() );

			// Load the properties

			File file = getPropertiesFile();
			progress.readingPropertyFile( file.getAbsolutePath() );
			this.properties = new Properties( defaultProperties );
			FileInputStream input = new FileInputStream( file );
			try
			{
				this.properties.load( input );
			}
			finally
			{
				input.close();
			}

			// Read the config version

			this.configVersion = 1;
			String s = this.properties.getProperty( "config-version" );
			if( s != null )
			{
				this.configVersion = Integer.parseInt( s );
				Assert.isTrue( this.configVersion >= 1 && this.configVersion <= 2, "config-version can only be 1 or 2" );
			}

			if( this.configVersion == 2 )
			{
				// Version 2 configuration

				// driver jars
				this.driverJars = new ArrayList();

				String driversProperty = this.properties.getProperty( "classpathext" );
				if( driversProperty != null )
					for( String driverJar : driversProperty.split( ";" ) )
					{
						driverJar = driverJar.trim();
						if( driverJar.length() > 0 )
							this.driverJars.add( driverJar );
					}

				if( pass > 1 )
				{
					String databaseConfigClass = this.properties.getProperty( "databases.config.class" );
					String databaseConfigScript = this.properties.getProperty( "databases.config.script" );

					if( databaseConfigClass != null ) // databases configuration plugin
					{
						try
						{
							Class cls = Class.forName( databaseConfigClass );
							DatabasesConfiguration config = (DatabasesConfiguration)cls.newInstance();
							config.init( this );
							this.databases = config.getDatabases();
						}
						catch( ClassNotFoundException e )
						{
							throw new SystemException( e );
						}
						catch( InstantiationException e )
						{
							throw new SystemException( e );
						}
						catch( IllegalAccessException e )
						{
							throw new SystemException( e );
						}
					}
					else if( databaseConfigScript != null ) // databases configuration script
					{
						// Use seperate GroovyUtil class to prevent linking to groovy when groovy is not needed.
						Map binding = new HashMap();
						binding.put( "configuration", this );
						this.databases = (List)GroovyUtil.evaluate( new File( databaseConfigScript ), binding );
						Assert.notNull( this.databases, "Did not receive a result from the databases configuration script" );
					}
					else
					{
						// read it myself
						this.databases = new ArrayList< Database >();

						String databasesProperty = this.properties.getProperty( "databases" );
						Assert.notBlank( databasesProperty, "'databases' not configured in " + DBPATCHER_PROPERTIES );
						for( String databaseName : databasesProperty.split( "," ) )
						{
							databaseName = databaseName.trim();
							if( databaseName.length() > 0 )
							{
								// per database
								String databaseDescription = this.properties.getProperty( databaseName + ".description" );
								String driver = this.properties.getProperty( databaseName + ".driver" );
								String dbUrl = this.properties.getProperty( databaseName + ".url" );

								if( StringUtils.isBlank( databaseDescription ) )
									databaseDescription = databaseName;
								Assert.notBlank( driver, "'" + databaseName + ".driver' not configured in " + DBPATCHER_PROPERTIES );
								Assert.notBlank( dbUrl, "'" + databaseName + ".url' not configured in " + DBPATCHER_PROPERTIES );

								Database database = new Database( databaseName, databaseDescription, driver, dbUrl );
								this.databases.add( database );

								// apps
								String appsProperty = this.properties.getProperty( databaseName + ".applications" );
								Assert.notBlank( appsProperty, "'" + databaseName + ".applications' not configured in " + DBPATCHER_PROPERTIES );

								for( String appName : appsProperty.split( "," ) )
								{
									appName = appName.trim();
									if( appName.length() > 0 )
									{
										String appDescription = this.properties.getProperty( databaseName + "." + appName + ".description" );
										String userName = this.properties.getProperty( databaseName + "." + appName + ".user" );
										String patchFile = this.properties.getProperty( databaseName + "." + appName + ".patchfile" );

										if( StringUtils.isBlank( appDescription ) )
											appDescription = appName;
										Assert.notBlank( userName, "'" + databaseName + "." + appName + ".user' not configured in " + DBPATCHER_PROPERTIES );
										Assert.notBlank( patchFile, "'" + databaseName + "." + appName + ".patchfile' not configured in " + DBPATCHER_PROPERTIES );

										database.addApplication( appName, appDescription, userName, null, patchFile, null );
									}
								}
							}
						}
					}

					Collections.sort( this.databases, new Database.Comparator() );
					for( Database database : this.databases )
						Collections.sort( database.applications, new Application.Comparator() );
				}
			}
			else
			{
				// Version 1 configuration

				this.dbDriverJar = this.properties.getProperty( "database.driver.jar" );
				this.dbDriver = this.properties.getProperty( "database.driver" );
				this.dbUrl = this.properties.getProperty( "database.url" );
				this.userName = this.properties.getProperty( "database.user" );

				Assert.isTrue( this.dbUrl != null, "database.url not found in " + DBPATCHER_PROPERTIES );
				Assert.isTrue( this.dbDriver != null, "database.driver not found in " + DBPATCHER_PROPERTIES );
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}


	public String getDBDriver()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriver;
	}

	public String getDBUrl()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbUrl;
	}

	public String getVersion()
	{
		return this.version;
	}

	public String getDBDriverJar()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriverJar;
	}

	public String getUser()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.userName;
	}

	public Database getDatabase( String name )
	{
		for( Database database : this.databases )
			if( database.name.equals( name ) )
				return database;
		throw new SystemException( "Database [" + name + "] not configured." );
	}

	public boolean isFromAnt()
	{
		return this.fromAnt;
	}

	public List< String > getDriverJars()
	{
		if( this.configVersion == 1 )
		{
			if( this.dbDriverJar != null )
				return Arrays.asList( this.dbDriverJar );
			return Collections.EMPTY_LIST;
		}
		return this.driverJars;
	}

	public List< Database > getDatabases()
	{
		Assert.isTrue( this.configVersion == 2, "Only supported with config version 2" );
		return this.databases;
	}

	public int getConfigVersion()
	{
		return this.configVersion;
	}

	public String getUserName()
	{
		return this.userName;
	}

	public String getPassWord()
	{
		return this.passWord;
	}

	public String getTarget()
	{
		return this.target;
	}

	public String getPatchFile()
	{
		return this.patchFile;
	}

	public String getProperty( String name )
	{
		return this.properties.getProperty( name );
	}
}
