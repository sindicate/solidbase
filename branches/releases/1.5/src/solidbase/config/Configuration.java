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
 * This class represents all the configuration.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static private final String DBPATCHER_PROPERTIES = "solidbase.properties";
	static private final String DBPATCHER_DEFAULT_PROPERTIES = "solidbase-default.properties";
	static private final String DBPATCHER_VERSION_PROPERTIES = "version.properties";

	/**
	 * The version of SolidBase.
	 */
	protected String version;

	/**
	 * Are we running the command line version of SolidBase within Apache Ant?
	 */
	protected boolean fromAnt;

	/**
	 * The contents of the properties file. Default this is solidbase.properties in the current folder, with missing
	 * properties coming from solidbase-default in the classpath.
	 */
	protected Properties properties;

	// Version 2 configuration

	/**
	 * A list of jars that need to be added to the classpath.
	 */
	protected List< String > driverJars;

	/**
	 * A list of configured databases.
	 */
	protected List< Database > databases;

	// Version 1 configuration

	/**
	 * Version of the properties format. Version 1 is deprecated, use version 2.
	 */
	protected int configVersion;

	/**
	 * Properties format 1: url of the database.
	 */
	protected String dbUrl;

	/**
	 * Properties format 1: driver class name for the database.
	 */
	protected String dbDriver;

	/**
	 * Properties format 1: jar file that needs to be added to the classpath.
	 */
	protected String dbDriverJar;

	/**
	 * Properties format 1: username for the database.
	 */
	protected String userName;

	/**
	 * Properties format 1: password for the database.
	 */
	protected String passWord;

	/**
	 * Properties format 1: target version to upgrade to.
	 */
	protected String target;

	/**
	 * Properties format 1: the upgrade file to be used.
	 */
	protected String patchFile;

	/**
	 * Returns the path of the properties file. Can be relative or absolute. Needed for testing.
	 * 
	 * @return the path of the properties file.
	 */
	protected File getPropertiesFile()
	{
		return new File( DBPATCHER_PROPERTIES );
	}

	/**
	 * Create a new configuration object. This constructor is used by the Ant Task and the Maven Plugin.
	 * 
	 * @param progress The listener that listens to config events.
	 */
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

	/**
	 * Create a new configuration object. This constructor is used by the command line version of SolidBase.
	 * 
	 * @param progress The listener that listens to config events.
	 * @param pass Are we in pass 1 or pass 2 of booting?
	 * @param optionDriver The optional driver class name for the database.
	 * @param optionUrl The optional url for the database.
	 * @param optionUserName The optional username for the database.
	 * @param optionPassWord The optional password for the database.
	 * @param optionTarget The optional target to upgrade to.
	 * @param optionPatchFile The optional upgrade file to use.
	 * @param optionPropertiesFile  The optional path of the properties file.
	 */
	// TODO The automatic target should also be added to the properties file
	public Configuration( ConfigListener progress, int pass, String optionDriver, String optionUrl, String optionUserName, String optionPassWord, String optionTarget, String optionPatchFile, String optionPropertiesFile )
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
			if( optionPropertiesFile != null )
				file = new File( optionPropertiesFile );
			else
				file = getPropertiesFile();
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
								if( appsProperty != null )
								{
									for( String appName : appsProperty.split( "," ) )
									{
										appName = appName.trim();
										if( appName.length() > 0 )
										{
											String appDescription = this.properties.getProperty( databaseName + "." + appName + ".description" );
											String userName = this.properties.getProperty( databaseName + "." + appName + ".username" );
											if( userName == null )
												userName = this.properties.getProperty( databaseName + "." + appName + ".user" );
											String patchFile = this.properties.getProperty( databaseName + "." + appName + ".upgradefile" );

											if( StringUtils.isBlank( appDescription ) )
												appDescription = appName;
											Assert.notBlank( userName, "'" + databaseName + "." + appName + ".username' not configured in " + DBPATCHER_PROPERTIES );
											Assert.notBlank( patchFile, "'" + databaseName + "." + appName + ".upgradefile' not configured in " + DBPATCHER_PROPERTIES );

											database.addApplication( appName, appDescription, userName, null, patchFile, null );
										}
									}
								}
								else
								{
									String appName = "default";
									String appDescription = appName;
									String userName = this.properties.getProperty( databaseName + ".username" );
									if( userName == null )
										userName = this.properties.getProperty( databaseName + ".user" );
									String patchFile = this.properties.getProperty( databaseName + ".upgradefile" );

									Assert.notBlank( userName, "'" + databaseName + ".username' not configured in " + DBPATCHER_PROPERTIES );
									Assert.notBlank( patchFile, "'" + databaseName + ".upgradefile' not configured in " + DBPATCHER_PROPERTIES );

									database.addApplication( appName, appDescription, userName, null, patchFile, null );
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
				this.userName = this.properties.getProperty( "database.username" );
				if( this.userName == null )
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

	/**
	 * Returns the driver class name.
	 * 
	 * @return The driver class name.
	 */
	public String getDBDriver()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriver;
	}

	/**
	 * Returns the url of the database.
	 * 
	 * @return The url of the database.
	 */
	public String getDBUrl()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbUrl;
	}

	/**
	 * Returns the version of SolidBase.
	 * 
	 * @return The version of SolidBase.
	 */
	public String getVersion()
	{
		return this.version;
	}

	/**
	 * Returns the jar file that contains the database driver.
	 * 
	 * @return the jar file that contains the database driver.
	 */
	public String getDBDriverJar()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriverJar;
	}

	/**
	 * Returns the user name for the database.
	 * 
	 * @return The user name for the database.
	 */
	public String getUser()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.userName;
	}

	/**
	 * Returns the database with the given name.
	 * 
	 * @param name The name for the database to return.
	 * @return The database with the given name. If the database is not found it throws a {@link SystemException}.
	 */
	public Database getDatabase( String name )
	{
		for( Database database : this.databases )
			if( database.name.equals( name ) )
				return database;
		throw new SystemException( "Database [" + name + "] not configured." );
	}

	/**
	 * Are we running the command line version from within Apache Ant?
	 * 
	 * @return True if we are running the command line version from within Apache Ant, false otherwise.
	 */
	public boolean isFromAnt()
	{
		return this.fromAnt;
	}

	/**
	 * Returns all the driver jar file names.
	 * 
	 * @return All the driver jar file names.
	 */
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

	/**
	 * Returns all the database.
	 * 
	 * @return All the database.
	 */
	public List< Database > getDatabases()
	{
		Assert.isTrue( this.configVersion == 2, "Only supported with config version 2" );
		return this.databases;
	}

	/**
	 * Returns the version of the properties format.
	 * 
	 * @return The version of the properties format.
	 */
	public int getConfigVersion()
	{
		return this.configVersion;
	}

	/**
	 * Returns the user name for the database.
	 * 
	 * @return The user name for the database.
	 */
	public String getUserName()
	{
		return this.userName;
	}

	/**
	 * Returns the password for the database.
	 * 
	 * @return The password for the database.
	 */
	public String getPassWord()
	{
		return this.passWord;
	}

	/**
	 * Returns the target to upgrade to.
	 * 
	 * @return The target to upgrade to.
	 */
	public String getTarget()
	{
		return this.target;
	}

	/**
	 * Returns the upgrade file to use.
	 * 
	 * @return The upgrade file to use.
	 */
	public String getPatchFile()
	{
		return this.patchFile;
	}

	/**
	 * Returns the property with the given name from the properties file.
	 * 
	 * @param name The name for the property.
	 * @return The property with the given name from the properties file.
	 */
	public String getProperty( String name )
	{
		return this.properties.getProperty( name );
	}
}
