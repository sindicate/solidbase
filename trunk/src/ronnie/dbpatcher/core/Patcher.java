package ronnie.dbpatcher.core;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
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
		patch( DBVersion.getVersion(), target );
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
	
	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = PatchFile.getPatches( version, target );
		Assert.check( patches != null );
		Assert.check( patches.size() > 0, "No patches found" );
		
//		System.out.println( "patch path for " + version + " to " + target + ":" );
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}
	
	static protected void patch( Patch patch ) throws SQLException
	{
		System.out.print( "Patching \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
		
		PatchFile.gotoPatch( patch );
		int skip = DBVersion.getStatements();
		if( DBVersion.getTarget() == null )
			skip = 0;
		
		Statement statement = Database.getConnection().createStatement();
		Command command = PatchFile.readStatement();
		int count = 0;
		while( command != null )
		{
			String sql = command.getCommand();
//			System.out.println( sql );
//			System.out.println();

			if( command.isInternal() )
			{
				Assert.fail( "Unknown command [" + sql + "]" );
			}
			else
			{
				if( sql.length() > 0 && count >= skip )
					statement.execute( sql ); // autocommit is on
				System.out.print( "." );
				if( !patch.isInit() )
					DBVersion.setCount( patch.getTarget(), ++count );
			}
			
			command = PatchFile.readStatement();
		}
		System.out.println();

		if( patch.isInit() )
			DBVersion.versionTablesCreated();
		if( !patch.isOpen() )
			DBVersion.setVersion( patch.getTarget() );
	}
}
