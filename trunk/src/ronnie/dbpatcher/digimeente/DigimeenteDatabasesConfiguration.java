package ronnie.dbpatcher.digimeente;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ronnie.dbpatcher.config.Configuration;
import ronnie.dbpatcher.config.Database;
import ronnie.dbpatcher.config.DatabasesConfiguration;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;

public class DigimeenteDatabasesConfiguration implements DatabasesConfiguration
{
	protected List< Database > databases;

	public void init( Configuration configuration )
	{
		try
		{
			List databases = new ArrayList();

			String location = configuration.getProperty( "databases.config.location" );
			if( location == null )
				throw new SystemException( "Property 'databases.config.location' not set." );

			File locationFile = new File( location );
			File[] folders = locationFile.listFiles();
			if( folders == null )
				throw new SystemException( "Folder does not exist: " + locationFile.getAbsolutePath() );

			for( File folder : folders )
				if( folder.isDirectory() )
					for( File file : folder.listFiles() )
						if( file.isFile() && file.getName().endsWith( ".properties" ) )
						{
							Properties properties = new Properties();
							InputStream in = new FileInputStream( file );
							try
							{
								properties.load( in );
							}
							finally
							{
								in.close();
							}

							String type = properties.getProperty( "conf.database.type" );
							String hostname = properties.getProperty( "conf.database.hostname" );
							String port = properties.getProperty( "conf.database.port" );
							String name = properties.getProperty( "conf.database.name" );
							String username = properties.getProperty( "conf.database.username" );

							Assert.notNull( type, "Property 'config.database.type' not found in: " + file.getAbsolutePath() );
							Assert.notNull( hostname, "Property 'config.database.hostname' not found in: " + file.getAbsolutePath() );
							Assert.notNull( port, "Property 'config.database.port' not found in: " + file.getAbsolutePath() );
							Assert.notNull( name, "Property 'config.database.name' not found in: " + file.getAbsolutePath() );
							Assert.notNull( username, "Property 'config.database.username' not found in: " + file.getAbsolutePath() );

							String driver;
							String url;
							String patchFile;
							if( type.equals( "oracle" ) )
							{
								driver = "oracle.jdbc.OracleDriver";
								url = "jdbc:oracle:thin:@" + hostname + ":" + port + ":" + name;
								patchFile = "dbpatch-oracle-example.sql";
							}
							else if( type.equals( "mysql" ) )
							{
								driver = "com.mysql.jdbc.Driver";
								url = "jdbc:mysql://" + hostname + ":" + port + "/" + name;
								patchFile = "dbpatch-mysql-example.sql";
							}
							else if( type.equals( "hsqldb" ) )
							{
								driver = "org.hsqldb.jdbcDriver";
								url = "jdbc:hsqldb:mem:" + name;
								patchFile = "dbpatch-hsqldb-example.sql";
							}
							else
								throw new SystemException( "Unknown database type: " + type );

							String fileName = file.getName();
							Database database = new Database( folder.getName() + "-" + fileName.substring( 0, fileName.length() - 11 ), null, driver, url );
							database.addApplication( "midoffice", null, username, patchFile );

							databases.add( database );
						}

			this.databases = databases;
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public List< Database > getDatabases()
	{
		return this.databases;
	}
}
