/*--
 * Copyright 2006 Ren� M. de Bloois
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
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import solidbase.core.FatalException;
import solidbase.core.SystemException;
import solidbase.util.Assert;
import solidstack.io.FatalIOException;


/**
 * This class represents all the configuration from the commandline or from the properties file.
 *
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static private final String SOLIDBASE_PROPERTIES = "solidbase.properties";
	static private final String SOLIDBASE_DEFAULT_PROPERTIES = "solidbase-default.properties";

	static private final Pattern propertyPattern = Pattern.compile( "^connection\\.([^\\s\\.]+)\\.(driver|url|username|password)$" );

	/**
	 * The contents of the properties file. Default this is solidbase.properties in the current folder, with missing
	 * properties coming from solidbase-default.properties in the classpath.
	 */
	protected Properties properties;

	/**
	 * A list of jars that need to be added to the classpath.
	 */
	protected List<String> driverJars;

	/**
	 * The default configured database.
	 */
	protected Database defaultDatabase;

	/**
	 * A list of configured secondary databases.
	 */
	protected Map<String, Database> secondaryDatabases = new HashMap<>();

	/**
	 * Target version to upgrade to.
	 */
	protected String target;

	/**
	 * The upgrade file to be used.
	 */
	protected String upgradeFile;

	/**
	 * The SQL file to be executed.
	 */
	protected String sqlFile;

	/**
	 * Parameters.
	 */
	protected Properties parameters;

	public Duration retryInterval;

	public Duration retryDuration;

	/**
	 * Returns the path of the properties file. Can be relative or absolute. Needed for testing.
	 *
	 * @return the path of the properties file.
	 */
	protected File getPropertiesFile() {
		return new File( SOLIDBASE_PROPERTIES );
	}

	/**
	 * Create a new configuration object. This constructor is used by the command line version of SolidBase.
	 *
	 * @param progress The listener that listens to config events.
	 * @param pass Are we in pass 1 or pass 2 of booting?
	 * @param options The options from the command line.
	 */
	public Configuration( ConfigListener progress, int pass, Options options ) {
		try {
			// Load the default properties

			URL url = Configuration.class.getResource( SOLIDBASE_DEFAULT_PROPERTIES );
			if( url == null ) {
				throw new SystemException( SOLIDBASE_DEFAULT_PROPERTIES + " not found in classpath" );
			}

			progress.readingConfigFile( url.toString() );

			properties = new Properties();
			try (InputStream input = url.openStream()) {
				properties.load( input );
			}

			// Load the solidbase.properties

			File file;
			if( options.config != null ) {
				file = new File( options.config );
			} else {
				file = getPropertiesFile();
			}

			if( file.exists() ) {
				progress.readingConfigFile( file.getAbsolutePath() );

				properties = new Properties( properties );
				try (InputStream input = new FileInputStream( file )) {
					properties.load( input );
				}

				// Read the config version

				String s = properties.getProperty( "properties-version" );
				if( !"1.0".equals( s ) ) {
					throw new FatalException( "Expecting properties-version 1.0 in the properties file" );
				}
			}

			// Load the commandline properties

			Properties commandLineProperties = new Properties( properties );
			if( options.driver != null ) {
				commandLineProperties.put( "connection.driver", options.driver );
			}
			if( options.url != null ) {
				commandLineProperties.put( "connection.url", options.url );
			}
			if( options.username != null ) {
				commandLineProperties.put( "connection.username", options.username );
			}
			if( options.password != null ) {
				commandLineProperties.put( "connection.password", options.password );
			}
			if( options.target != null ) {
				commandLineProperties.put( "upgrade.target", options.target );
			}
			if( options.upgradefile != null ) {
				commandLineProperties.put( "upgrade.file", options.upgradefile );
			}
			if( options.sqlfile != null ) {
				commandLineProperties.put( "sql.file", options.sqlfile );
			}
			if( options.retryInterval != null ) {
				commandLineProperties.put( "connections.retry.interval.seconds", options.retryInterval );
			}
			if( options.retryDuration != null ) {
				commandLineProperties.put( "connections.retry.duration.seconds", options.retryDuration );
			}
			if( !commandLineProperties.isEmpty() ) {
				properties = commandLineProperties;
			}
			parameters = options.parameters;

			// Read the classpath extension

			driverJars = new ArrayList<>();
			String driversProperty = properties.getProperty( "classpath.ext" );
			if( driversProperty != null ) {
				for( String driverJar : driversProperty.split( ";" ) ) {
					driverJar = driverJar.trim();
					if( driverJar.length() > 0 ) {
						driverJars.add( driverJar );
					}
				}
			}

			if( pass > 1 ) {
				String driver = properties.getProperty( "connection.driver" );
				String dbUrl = properties.getProperty( "connection.url" );
				String userName = properties.getProperty( "connection.username" );
				String password = properties.getProperty( "connection.password" );
				String upgradeFile = properties.getProperty( "upgrade.file" );
				String target = properties.getProperty( "upgrade.target" );
				String sqlFile = properties.getProperty( "sql.file" );
				String retryInterval = properties.getProperty( "connections.retry.interval.seconds" );
				String retryDuration= properties.getProperty( "connections.retry.duration.seconds" );

				if (retryInterval != null) {
					this.retryInterval = toDuration( retryInterval );
				}
				if (retryDuration != null) {
					this.retryDuration = toDuration( retryDuration );
				}

				if( driver != null || dbUrl != null || userName != null || password != null ) {
					defaultDatabase = new Database( "default", driver, dbUrl, userName, password );
				}
				this.upgradeFile = upgradeFile;
				this.target = target;
				this.sqlFile = sqlFile;

				for( Entry<Object, Object> entry : properties.entrySet() ) {
					String key = (String)entry.getKey();
					Matcher matcher = propertyPattern.matcher( key );
					if( matcher.matches() ) {
						String name = matcher.group( 1 );
						String prop = matcher.group( 2 );
						String value = (String)entry.getValue();
						Database database = secondaryDatabases.get( name );
						if( database == null ) {
							database = new Database( name );
							secondaryDatabases.put( name, database );
						}
						if( prop.equals( "driver" ) ) {
							database.setDriver( value );
						} else if( prop.equals( "url" ) ) {
							database.setUrl( value );
						} else if( prop.equals( "username" ) ) {
							database.setUserName( value );
						} else if( prop.equals( "password" ) ) {
							database.setPassword( value );
						} else {
							Assert.fail();
						}
					} else if( key.startsWith( "parameter." ) ) {
						key = key.substring( 10 );
						if( !parameters.containsKey( key ) ) {
							parameters.put( key, entry.getValue() );
						}
					}
				}

				// Validate them

				for( Database database : secondaryDatabases.values() ) {
					if( database.getName().equals( "default" ) ) {
						throw new FatalException( "The secondary connection name 'default' is not allowed" );
					}
					if( StringUtils.isBlank( database.getUserName() ) ) {
						throw new FatalException( "Property 'connection." + database.getName() + ".username' must be specified in " + SOLIDBASE_PROPERTIES );
					}
				}
			}

		} catch( IOException e ) {
			throw new FatalIOException( e );
		}
	}

	private Duration toDuration(String value) {
		try {
			long l = Long.parseLong( value );
			if (l < 0) {
				throw new FatalException( "Value '" + value + "' is not a duration in seconds" );
			}
			return Duration.ofSeconds( l );
		} catch (NumberFormatException e) {
			throw new FatalException( "Value '" + value + "' is not a duration in seconds" );
		}
	}

	/**
	 * Returns all the driver jar file names.
	 *
	 * @return All the driver jar file names.
	 */
	public List<String> getDriverJars() {
		return driverJars;
	}

	/**
	 * Returns all the database.
	 *
	 * @return All the database.
	 */
	public Collection<Database> getSecondaryDatabases() {
		return secondaryDatabases.values();
	}

	/**
	 * Returns the default database.
	 *
	 * @return The default database.
	 */
	public Database getDefaultDatabase() {
		return defaultDatabase;
	}

	/**
	 * Returns the target to upgrade to.
	 *
	 * @return The target to upgrade to.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Returns the upgrade file to use.
	 *
	 * @return The upgrade file to use.
	 */
	public String getUpgradeFile() {
		return upgradeFile;
	}

	/**
	 * Returns the upgrade file to use.
	 *
	 * @return The upgrade file to use.
	 */
	public String getSqlFile() {
		return sqlFile;
	}

	/**
	 * Returns the property with the given name from the properties file.
	 *
	 * @param name The name for the property.
	 * @return The property with the given name from the properties file.
	 */
	public String getProperty( String name ) {
		return properties.getProperty( name );
	}

	/**
	 * @return The parameters.
	 */
	public Properties getParameters() {
		return parameters;
	}

	public Duration getRetryInterval() {
		return retryInterval;
	}

	public Duration getRetryDuration() {
		return retryDuration;
	}

	/**
	 * Returns the first configuration error for display.
	 *
	 * @return The first configuration error.
	 */
	public String getFirstError() {
		Database database = defaultDatabase;
		if( database != null ) {
			if( StringUtils.isBlank( database.driver ) ) {
				return "Missing 'connection.driver' or -driver";
			}
			if( StringUtils.isBlank( database.url ) ) {
				return "Missing 'connection.url' or -url";
			}
			if( StringUtils.isBlank( database.userName ) ) {
				return "Missing 'connection.username' or -username";
			}
			if( StringUtils.isBlank( upgradeFile ) && StringUtils.isBlank( sqlFile ) ) {
				return "Missing 'upgrade.file', -upgradefile, 'sql.file' or -sqlfile";
			}
			if( !StringUtils.isBlank( sqlFile ) && !StringUtils.isBlank( target ) ) {
				return "'upgrade.target', -target is not allowed in combination with 'sql.file', -sqlfile";
			}

			for( Database secondary : secondaryDatabases.values() ) {
				// Driver and url are inherited, so they can be null but not blank
				if( StringUtils.isWhitespace( secondary.driver ) ) {
					return "Missing 'connection." + secondary.name + ".driver'";
				}
				if( StringUtils.isWhitespace( secondary.url ) ) {
					return "Missing 'connection." + secondary.name + ".url'";
				}
				if( StringUtils.isBlank( secondary.userName ) ) {
					return "Missing 'connection." + secondary.name + ".username'";
				}
			}
		} else {
			if( !StringUtils.isBlank( sqlFile ) ) {
				return "'sql.file' property or -sqlfile commandline option specified but no database configured";
			}
			if( !StringUtils.isBlank( upgradeFile ) ) {
				return "'upgrade.file' property or -upgradefile commandline option specified but no database configured";
			}
			if( !StringUtils.isBlank( target ) ) {
				return "'upgrade.target' property or -target commandline option specified but no database configured";
			}
		}

		return null;
	}

	/**
	 * Determines if the configuration is void.
	 *
	 * @return True if the configuration is void, false otherwise.
	 */
	public boolean isVoid() {
		return defaultDatabase == null && upgradeFile == null && target == null;
	}

}
