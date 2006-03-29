package ronnie.dbpatcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;




/**
 * Represents the database.
 */
public class Database
{
	/**
	 * The connection to the database.
	 */
	static protected Connection connection;
	
	/**
	 * Returns (and establishes) the connection to the database.
	 *  
	 * @return the connection
	 */
	static protected Connection getConnection()
	{
		if( connection == null )
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
				connection = DriverManager.getConnection( "jdbc:derby:c:/projects/java/dbpatcher/derbyDB;create=true" );
				connection.setAutoCommit( true );
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
		return connection;
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
		
		Statement statement = connection.createStatement();
		Command command = PatchFile.readStatement();
		int count = 0;
		while( command != null )
		{
			String sql = command.getCommand();
			System.out.println( sql );
			System.out.println();

			if( command.isInternal() )
			{
				Assert.fail( "Unknown command [" + sql + "]" );
			}
			else
			{
				if( sql.length() > 0 )
					statement.execute( sql ); // autocommit is on
				if( !patch.isInit() )
					DBVersion.setCount( patch.getTarget(), ++count );
			}
			
			command = PatchFile.readStatement();
		}

		if( patch.isInit() )
			DBVersion.versionTablesCreated();
		if( !patch.isOpen() )
			DBVersion.setVersion( patch.getTarget() );
	}
}
