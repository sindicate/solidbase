package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.cmg.pas.SystemException;


/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:16:37 PM
 */
public class Database
{
	static protected String driverName;
	static protected String url;
	static protected Connection connection;
	
	static protected void setConnection( String driverName, String url )
	{
		Database.driverName = driverName;
		Database.url = url;
	}
	
	static protected void setConnection( Connection connection )
	{
		Database.connection = connection;
		try
		{
			connection.setAutoCommit( true );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
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
}
