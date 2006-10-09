package ronnie.dbpatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
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
	static private final String DBPATCHER_DEFAULT_PROPERTIES_OLD = "private.properties";
	
	static protected String dbUrl; 
	static protected String dbDriver; 
	static protected String dbDriverJar; 
	static protected String schema; 
	static protected String version; 
	static protected String user; 
	
	static
	{
		try
		{
			// Load the default properties or the old private properties
			URL url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES );
			if( url == null )
			{
				url = Configuration.class.getResource( DBPATCHER_DEFAULT_PROPERTIES_OLD );
				if( url == null )
					throw new FileNotFoundException( DBPATCHER_DEFAULT_PROPERTIES + " or " + DBPATCHER_DEFAULT_PROPERTIES_OLD + " not found in classpath" );
			}
			Console.println( "Reading property file " + url );
			Properties defaultProperties = new Properties();
			defaultProperties.load( url.openStream() );
			
			// Load the properties
			File file = new File( DBPATCHER_PROPERTIES );
			Console.println( "Reading property file " + file.getAbsolutePath() );
			FileInputStream input = new FileInputStream( file );
			Properties properties = new Properties( defaultProperties );
			properties.load( input );
			
			dbUrl = properties.getProperty( "database.url" );
			dbDriverJar = properties.getProperty( "database.driver.jar" );
			dbDriver = properties.getProperty( "database.driver" );
			schema = properties.getProperty( "version.schema" );
			version = properties.getProperty( "patcher.version" );
			user = properties.getProperty( "database.user" );
			if( schema.length() == 0 )
				schema = null;
			
			Assert.isTrue( dbUrl != null, "database.url not found in dbpatcher.properties" );
			Assert.isTrue( dbDriver != null, "database.driver not found in dbpatcher.properties" );
			Assert.isTrue( version != null, "patcher.version not found in dbpatcher.properties" );
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
}
