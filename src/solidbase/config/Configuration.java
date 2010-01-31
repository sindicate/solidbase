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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import solidbase.core.Assert;
import solidbase.core.FatalException;
import solidbase.core.SystemException;


/**
 * This class represents all the configuration from the commandline or from the properties file.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static private final String DBPATCHER_PROPERTIES = "solidbase.properties";
	static private final String DBPATCHER_DEFAULT_PROPERTIES = "solidbase-default.properties";

	static private final Pattern propertyPattern = Pattern.compile( "^connection\\.([^\\s\\.]+)\\.(driver|url|username|password)$" );

	/**
	 * Are we running the command line version of SolidBase within Apache Ant?
	 */
	protected boolean fromAnt;

	/**
	 * The contents of the properties file. Default this is solidbase.properties in the current folder, with missing
	 * properties coming from solidbase-default in the classpath.
	 */
	protected Properties properties;

	/**
	 * A list of jars that need to be added to the classpath.
	 */
	protected List< String > driverJars;

	/**
	 * A list of configured databases.
	 */
	protected Map< String, Database > secondaryDatabases = new HashMap< String, Database >();

	/**
	 * The default database.
	 */
	protected Database defaultDatabase;

	/**
	 * Target version to upgrade to.
	 */
	protected String target;

	/**
	 * The upgrade file to be used.
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
		try
		{
			// Load the default properties

			URL url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
				throw new SystemException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

			progress.readingConfigFile( url.toString() );
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

		try
		{
			// Process the commandline options

			if( optionDriver != null )
			{
				this.defaultDatabase = new Database( "default", optionDriver, optionUrl, optionUserName, optionPassWord );
				this.target = optionTarget;
				this.patchFile = optionPatchFile;
				return; // No need to read the properties
			}

			// Load the default properties

			URL url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
				throw new SystemException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

			progress.readingConfigFile( url.toString() );
			Properties defaultProperties = new Properties();
			defaultProperties.load( url.openStream() );

			// Load the properties

			File file = getPropertiesFile();
			if( optionPropertiesFile != null )
				file = new File( optionPropertiesFile );
			else
				file = getPropertiesFile();
			progress.readingConfigFile( file.getAbsolutePath() );
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

			String s = this.properties.getProperty( "properties-version" );
			if( !"1.0".equals( s ) )
				throw new FatalException( "Expecting properties-version 1.0 in the properties file" );

			this.driverJars = new ArrayList();
			String driversProperty = this.properties.getProperty( "classpath.ext" );
			if( driversProperty != null )
				for( String driverJar : driversProperty.split( ";" ) )
				{
					driverJar = driverJar.trim();
					if( driverJar.length() > 0 )
						this.driverJars.add( driverJar );
				}

			if( pass > 1 )
			{
				String driver = this.properties.getProperty( "connection.driver" );
				String dbUrl = this.properties.getProperty( "connection.url" );
				String userName = this.properties.getProperty( "connection.username" );
				String password = this.properties.getProperty( "connection.password" );
				String patchFile = this.properties.getProperty( "upgrade.file" );
				String target = this.properties.getProperty( "upgrade.target" );

				if( StringUtils.isBlank( driver ) )
					throw new FatalException( "Property 'connection.driver' must be specified in " + DBPATCHER_PROPERTIES );
				if( StringUtils.isBlank( dbUrl ) )
					throw new FatalException( "Property 'connection.url' must be specified in " + DBPATCHER_PROPERTIES );
				if( StringUtils.isBlank( userName ) )
					throw new FatalException( "Property 'connection.username' must be specified in " + DBPATCHER_PROPERTIES );
				if( StringUtils.isBlank( patchFile ) )
					throw new FatalException( "Property 'upgrade.file' must be specified in " + DBPATCHER_PROPERTIES );
				if( StringUtils.isBlank( target ) )
					throw new FatalException( "Property 'upgrade.target' must be specified in " + DBPATCHER_PROPERTIES );

				this.defaultDatabase = new Database( "default", driver, dbUrl, userName, password );
				this.patchFile = patchFile;
				this.target = target;

				for( Entry entry : this.properties.entrySet() )
				{
					String key = (String)entry.getKey();
					Matcher matcher = propertyPattern.matcher( key );
					if( matcher.matches() )
					{
						String name = matcher.group( 1 );
						String prop = matcher.group( 2 );
						String value = (String)entry.getValue();
						Database database = this.secondaryDatabases.get( name );
						if( database == null )
						{
							database = new Database( name );
							this.secondaryDatabases.put( name, database );
						}
						if( prop.equals( "driver" ) )
							database.setDriver( value );
						else if( prop.equals( "url" ) )
							database.setUrl( value );
						else if( prop.equals( "username" ) )
							database.setUserName( value );
						else if( prop.equals( "password" ) )
							database.setPassword( value );
						else
							Assert.fail();
					}
				}

				// Validate them
				for( Database database : this.secondaryDatabases.values() )
				{
					if( database.getName().equals( "default" ) )
						throw new FatalException( "The secondary connection name 'default' is not allowed" );
					if( StringUtils.isBlank( database.getUserName() ) )
						throw new FatalException( "Property 'connection." + database.getName() + ".username' must be specified in " + DBPATCHER_PROPERTIES );
				}
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
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
		return this.driverJars;
	}

	/**
	 * Returns all the database.
	 * 
	 * @return All the database.
	 */
	public Collection< Database > getSecondaryDatabases()
	{
		return this.secondaryDatabases.values();
	}

	/**
	 * Returns the default database.
	 * 
	 * @return The default database.
	 */
	public Database getDefaultDatabase()
	{
		return this.defaultDatabase;
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
