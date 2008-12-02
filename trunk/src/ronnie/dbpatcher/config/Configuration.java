package ronnie.dbpatcher.config;

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

	protected String version;
	protected boolean fromAnt;

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

	// TODO The automatic target should also be added to the properties file
	public Configuration( ConfigListener progress, String optionDriver, String optionUrl, String optionUserName, String optionPassWord, String optionTarget, String optionPatchFile ) throws IOException
	{
		// Checks

		if( optionDriver != null )
			Assert.isTrue( optionUrl != null && optionUserName != null );
		else
			Assert.isTrue( optionUrl == null && optionUserName == null );

		// Load the version properties

		URL url = Configuration.class.getResource( DBPATCHER_VERSION_PROPERTIES );
		if( url == null )
			throw new FileNotFoundException( DBPATCHER_VERSION_PROPERTIES );

		Properties properties = new Properties();
		properties.load( url.openStream() );

		this.version = properties.getProperty( "module.version" );

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
			throw new FileNotFoundException( DBPATCHER_DEFAULT_PROPERTIES + " not found in classpath" );

		progress.readingPropertyFile( url.toString() );
		Properties defaultProperties = new Properties();
		defaultProperties.load( url.openStream() );

		// Load the properties

		File file = new File( DBPATCHER_PROPERTIES );
		progress.readingPropertyFile( file.getAbsolutePath() );
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

		this.configVersion = 1;
		String s = properties.getProperty( "config-version" );
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

			String driversProperty = properties.getProperty( "driverjars" );
			Assert.notBlank( driversProperty, "'driverjars' not configured in " + DBPATCHER_PROPERTIES );
			for( String driverJar : driversProperty.split( "," ) )
			{
				driverJar = driverJar.trim();
				if( driverJar.length() > 0 )
					this.driverJars.add( driverJar );
			}

			// databases
			this.databases = new ArrayList< Database >();

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
					this.databases.add( database );

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

			this.dbDriverJar = properties.getProperty( "database.driver.jar" );
			this.dbDriver = properties.getProperty( "database.driver" );
			this.dbUrl = properties.getProperty( "database.url" );
			this.userName = properties.getProperty( "database.user" );

			Assert.isTrue( this.dbUrl != null, "database.url not found in " + DBPATCHER_PROPERTIES );
			Assert.isTrue( this.dbDriver != null, "database.driver not found in " + DBPATCHER_PROPERTIES );
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
		Assert.isTrue( this.configVersion == 2, "Only supported with config version 2" );
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

	public String getDbUrl()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbUrl;
	}

	public String getDbDriver()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriver;
	}

	public String getDbDriverJar()
	{
		Assert.isTrue( this.configVersion == 1, "Only supported with config version 1" );
		return this.dbDriverJar;
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


	static public class Database
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

		public Application getApplication( String name )
		{
			for( Application application : this.applications )
				if( application.name.equals( name ) )
					return application;
			throw new SystemException( "Application [" + name + "] not configured for database [" + this.name + "]." );
		}

		public String getName()
		{
			return this.name;
		}

		public String getDescription()
		{
			return this.description;
		}

		public String getDriver()
		{
			return this.driver;
		}

		public String getUrl()
		{
			return this.url;
		}

		public List< Application > getApplications()
		{
			return this.applications;
		}
	}


	static public class Application
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

		public String getName()
		{
			return this.name;
		}

		public String getDescription()
		{
			return this.description;
		}

		public String getUserName()
		{
			return this.userName;
		}

		public String getPatchFile()
		{
			return this.patchFile;
		}
	}
}
