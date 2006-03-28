package ronnie.dbpatcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

public class Database
{
	static protected Connection connection2;
	
	static protected Connection getConnection()
	{
		if( connection2 == null )
		{
			try
			{
				Class.forName( "org.apache.derby.jdbc.EmbeddedDriver" );
			}
			catch( ClassNotFoundException e )
			{
				throw new SystemException( e );
			}
			try
			{
				Database.connection2 = DriverManager.getConnection( "jdbc:derby:c:/projects/java/dbpatcher/derbyDB;create=true" );
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
		return connection2;
	}
	
	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = PatchFile.getPatches( version, target );
		Assert.check( patches != null, "no path found for " + version + " to " + target );
		
//		System.out.println( "patch path for " + version + " to " + target + ":" );
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}
	
	static protected void patch( Patch patch ) throws SQLException
	{
		System.out.println( patch.getSource() + " --> " + patch.getTarget() );
		
		PatchFile.gotoPatch( patch );
		
		Statement statement = connection2.createStatement();
		String sql = PatchFile.readStatement();
		while( sql != null )
		{
			System.out.println( sql );
			System.out.println();

			statement.execute( sql );
			connection2.commit();
			
			sql = PatchFile.readStatement();
		}
	}
}
