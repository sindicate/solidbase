package ronnie.dbpatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Configuration
{
	static protected String url; 
	static protected String driver; 
	static protected String driverJar; 
	static protected String schema; 
	static protected String version; 
	
	static
	{
		try
		{
			File file = new File( "dbpatcher.properties" );
			System.out.println( "Reading property file " + file.getAbsolutePath() );
			FileInputStream input = new FileInputStream( file );
			Properties properties = new Properties();
			properties.load( input );
			url = properties.getProperty( "database.url" );
			driverJar = properties.getProperty( "database.driver.jar" );
			
			Assert.check( url != null, "database.url not specified in dbpatcher.properties" );

			URL url = Configuration.class.getResource( "private.properties" );
			if( url == null )
				throw new FileNotFoundException( "private.properties not found in classpath" );
			System.out.println( "Reading property file " + url );
			properties = new Properties();
			properties.load( url.openStream() );
			driver = properties.getProperty( "database.driver" );
			schema = properties.getProperty( "version.schema" );
			version = properties.getProperty( "patcher.version" );
			if( schema.length() == 0 )
				schema = null;
			
			Assert.check( driver != null, "database.driver not specified in private.properties" );
			Assert.check( version != null, "patcher.version not specified in private.properties" );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
	
	static protected String getDriver()
	{
		return driver;
	}
	
	static protected String getDBUrl()
	{
		return url;
	}

	static protected String getVersion()
	{
		return version;
	}

	static protected String getDriverJar()
	{
		return driverJar;
	}
}
