package ronnie.dbpatcher.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

public class Patcher
{
	static public void openPatchFile() throws IOException
	{
		PatchFile.open();
	}
	
	static public void readPatchFile() throws IOException
	{
		PatchFile.read();
	}
	
	static public void closePatchFile() throws IOException
	{
		PatchFile.close();
	}
	
	static public String getCurrentVersion()
	{
		return DBVersion.getVersion();
	}
	
	static public String getCurrentTarget()
	{
		return DBVersion.getTarget();
	}
	
	static public int getCurrentStatements()
	{
		return DBVersion.getStatements();
	}
	
	static public List getTargets()
	{
		return 	PatchFile.getTargets( DBVersion.getVersion() );
	}
	
	static public void patch( String target ) throws SQLException
	{
		Database.patch( DBVersion.getVersion(), target );
	}

	static public void setConnection( String driverName, String url )
	{
		Database.setConnection( driverName, url );
	}
	
	static public void setConnection( Connection connection )
	{
		Database.setConnection( connection );
	}
	
	static public String getVersion()
	{
		Properties properties = new Properties();
		try
		{
			properties.load( Patcher.class.getResourceAsStream( "core.properties" ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		String result = properties.getProperty( "core.version" );
		Assert.check( result != null );
		return result;
	}
}
