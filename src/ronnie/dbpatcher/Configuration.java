package ronnie.dbpatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;

/**
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static private final String DBPATCHER_PROPERTIES = "dbpatcher.properties";
	static private final String DBPATCHER_DEFAULT_PROPERTIES = "dbpatcher-default.properties";
	static private final String DBPATCHER_VERSION_PROPERTIES = "version.properties";

	static protected String version;
	static protected boolean fromAnt;

	// Version 2 configuration
	static protected String driverJars;
	static protected List< Database > databases;

	// Version 1 configuration
	static protected int configVersion;
	static protected String dbUrl;
	static protected String dbDriver;
	static protected String dbDriverJar;
	static protected String user;

	static
	{
		try
		{
			// Load the version properties

			URL url = Configuration.class.getResource( DBPATCHER_VERSION_PROPERTIES );
			if( url == null )
				throw new FileNotFoundException( DBPATCHER_VERSION_PROPERTIES );

			Properties properties = new Properties();
			properties.load( url.openStream() );

			version = properties.getProperty( "module.version" );

			Assert.isTrue( version != null, "module.version not found in version.properties" );

			// Load the default properties

			url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
				throw new FileNotFoundException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

			Console.println( "Reading property file " + url );
			Properties defaultProperties = new Properties();
			defaultProperties.load( url.openStream() );

			// Load the properties

			File file = new File( DBPATCHER_PROPERTIES );
			Console.println( "Reading property file " + file.getAbsolutePath() );
			properties = new Properties( defaultProperties );
			FileInputStream input = new FileInputStream( file );
			try
			{
				properties.load( input );
			}
			finally
			{
				input.close();
			}

			// Read the config version

			configVersion = 1;
			String s = properties.getProperty( "config-version" );
			if( s != null )
			{
				configVersion = Integer.parseInt( s );
				Assert.isTrue( configVersion >= 1 && configVersion <= 2, "config-version can only be 1 or 2" );
			}

			if( configVersion == 2 )
			{
				// Version 2 configuration

				// driver jars
				driverJars = properties.getProperty( "driverjars" );

				// databases
				databases = new ArrayList< Database >();

				String databasesProperty = properties.getProperty( "databases" );
				Assert.notBlank( databasesProperty, "'databases' not configured in " + DBPATCHER_PROPERTIES );
				for( String databaseName : databasesProperty.split( "," ) )
				{
					databaseName = databaseName.trim();
					if( databaseName.length() > 0 )
					{
						// per database
						String name = properties.getProperty( databaseName + ".name" );
						String driver = properties.getProperty( databaseName + ".driver" );
						String dbUrl = properties.getProperty( databaseName + ".url" );

						Assert.notBlank( name, "'" + databaseName + ".name' not configured in " + DBPATCHER_PROPERTIES );
						Assert.notBlank( driver, "'" + databaseName + ".driver' not configured in " + DBPATCHER_PROPERTIES );
						Assert.notBlank( dbUrl, "'" + databaseName + ".url' not configured in " + DBPATCHER_PROPERTIES );

						Database database = new Database( name, driver, dbUrl );
						databases.add( database );

						// apps
						String appsProperty = properties.getProperty( database + ".applications" );
						Assert.notBlank( appsProperty, "'" + database + ".applications' not configured in " + DBPATCHER_PROPERTIES );

						for( String app : appsProperty.split( "," ) )
						{
							app = app.trim();
							if( app.length() > 0 )
							{
								String userName = properties.getProperty( database + "." + app + ".user" );
								Assert.notBlank( name, "'" + database + "." + app + ".user' not configured in " + DBPATCHER_PROPERTIES );

								database.addApplication( userName );
							}
						}
					}
				}
			}
			else
			{
				// Version 1 configuration

				dbUrl = properties.getProperty( "database.url" );
				dbDriverJar = properties.getProperty( "database.driver.jar" );
				dbDriver = properties.getProperty( "database.driver" );
				user = properties.getProperty( "database.user" );

				Assert.isTrue( dbUrl != null, "database.url not found in " + DBPATCHER_PROPERTIES );
				Assert.isTrue( dbDriver != null, "database.driver not found in " + DBPATCHER_PROPERTIES );

			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	static protected String getDBDriver()
	{
		return dbDriver;
	}

	static protected String getDBUrl()
	{
		return dbUrl;
	}

	static protected String getVersion()
	{
		return version;
	}

	static protected String getDBDriverJar()
	{
		return dbDriverJar;
	}

	static protected String getUser()
	{
		return user;
	}

	static protected class Database
	{
		protected String name;
		protected String driver;
		protected String url;
		protected List< Application > applications;
		protected Database( String name, String driver, String url )
		{
			this.name = name;
			this.driver = driver;
			this.url = url;
		}
		protected void addApplication( String userName )
		{
			this.applications.add( new Application( userName ) );
		}
	}

	static protected class Application
	{
		protected String name;
		protected String userName;
		protected Application( String userName )
		{
			this.userName = userName;
		}
	}
}
