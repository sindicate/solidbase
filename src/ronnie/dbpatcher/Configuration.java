package ronnie.dbpatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

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
	static protected List< String > driverJars;
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
				driverJars = new ArrayList();

				String driversProperty = properties.getProperty( "driverjars" );
				Assert.notBlank( driversProperty, "'driverjars' not configured in " + DBPATCHER_PROPERTIES );
				for( String driverJar : driversProperty.split( "," ) )
				{
					driverJar = driverJar.trim();
					if( driverJar.length() > 0 )
						driverJars.add( driverJar );
				}

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
						String databaseDescription = properties.getProperty( databaseName + ".description" );
						String driver = properties.getProperty( databaseName + ".driver" );
						String dbUrl = properties.getProperty( databaseName + ".url" );

						if( StringUtils.isBlank( databaseDescription ) )
							databaseDescription = databaseName;
						Assert.notBlank( driver, "'" + databaseName + ".driver' not configured in " + DBPATCHER_PROPERTIES );
						Assert.notBlank( dbUrl, "'" + databaseName + ".url' not configured in " + DBPATCHER_PROPERTIES );

						Database database = new Database( databaseName, databaseDescription, driver, dbUrl );
						databases.add( database );

						// apps
						String appsProperty = properties.getProperty( databaseName + ".applications" );
						Assert.notBlank( appsProperty, "'" + databaseName + ".applications' not configured in " + DBPATCHER_PROPERTIES );

						for( String appName : appsProperty.split( "," ) )
						{
							appName = appName.trim();
							if( appName.length() > 0 )
							{
								String appDescription = properties.getProperty( databaseName + "." + appName + ".description" );
								String userName = properties.getProperty( databaseName + "." + appName + ".user" );
								String patchFile = properties.getProperty( databaseName + "." + appName + ".patchfile" );

								if( StringUtils.isBlank( appDescription ) )
									appDescription = appName;
								Assert.notBlank( userName, "'" + databaseName + "." + appName + ".user' not configured in " + DBPATCHER_PROPERTIES );
								Assert.notBlank( patchFile, "'" + databaseName + "." + appName + ".patchfile' not configured in " + DBPATCHER_PROPERTIES );

								database.addApplication( appName, appDescription, userName, patchFile );
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

	static protected Database getDatabase( String name )
	{
		for( Database database : databases )
			if( database.name.equals( name ) )
				return database;
		throw new SystemException( "Database [" + name + "] not configured." );
	}

	static protected class Database
	{
		protected String name;
		protected String description;
		protected String driver;
		protected String url;
		protected List< Application > applications = new ArrayList();
		protected Database( String name, String description, String driver, String url )
		{
			this.name = name;
			this.description = description;
			this.driver = driver;
			this.url = url;
		}
		protected void addApplication( String name, String description, String userName, String patchFile )
		{
			this.applications.add( new Application( name, description, userName, patchFile ) );
		}
		protected Application getApplication( String name )
		{
			for( Application application : this.applications )
				if( application.name.equals( name ) )
					return application;
			throw new SystemException( "Application [" + name + "] not configured for database [" + this.name + "]." );
		}
	}

	static protected class Application
	{
		protected String name;
		protected String description;
		protected String userName;
		protected String patchFile;
		protected Application( String name, String description, String userName, String patchFile )
		{
			this.name = name;
			this.description = description;
			this.userName = userName;
			this.patchFile = patchFile;
		}
	}
}
