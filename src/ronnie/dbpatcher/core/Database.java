package ronnie.dbpatcher.core;

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
	static protected String driverName;
	static protected String url;
	static protected Connection connection;
	
	static protected void setDatabase( String driverName, String url )
	{
		Database.driverName = driverName;
		Database.url = url;
	}
	
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
				Class.forName( driverName );
			}
			catch( ClassNotFoundException e )
			{
				throw new SystemException( e );
			}
			
			try
			{
				connection = DriverManager.getConnection( url );
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
		System.out.println( "Patching \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
		
		PatchFile.gotoPatch( patch );
		int skip = DBVersion.getStatements();
		if( DBVersion.getTarget() == null )
			skip = 0;
		
		Statement statement = connection.createStatement();
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
