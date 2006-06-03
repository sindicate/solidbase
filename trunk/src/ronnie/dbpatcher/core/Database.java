package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;


/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:16:37 PM
 */
public class Database
{
	static protected String driverName;
	static protected String url;
	static protected HashMap connections = new HashMap();
	static protected HashMap passwords = new HashMap();
	static protected String defaultUser;
	
	static protected void setConnection( String driverName, String url )
	{
		Database.driverName = driverName;
		Database.url = url;
		try
		{
			Class.forName( driverName );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}
	}
	
//	static protected void setConnection( Connection connection )
//	{
//		Database.connection = connection;
//		try
//		{
//			connection.setAutoCommit( true );
//		}
//		catch( SQLException e )
//		{
//			throw new SystemException( e );
//		}
//	}
	
	static protected Connection getConnection( String url, String user, String password )
	{
		try
		{
			Connection connection = DriverManager.getConnection( url, user, password );
			connection.setAutoCommit( true );
			return connection;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}
	
	static protected Connection getConnection( String user )
	{
		Connection connection = (Connection)connections.get( user );
		if( connection == null )
		{
			String password = Patcher.callBack.requestPassword( user );
			connection = getConnection( url, user, password );
			connections.put( user, connection );
			passwords.put( user, password );
		}
		return connection;
	}
	
	static protected Connection getConnection()
	{
		Assert.notNull( defaultUser, "User not set" );
		return getConnection( defaultUser );
	}

	static protected Connection getNewConnection()
	{
		Assert.notNull( defaultUser, "User not set" );
		String password = (String)passwords.get( defaultUser );
		Assert.notNull( password, "Password not set" );
		return getConnection( url, defaultUser, password );
	}

	static protected void setDefaultUser( String defaultUser )
	{
		Database.defaultUser = defaultUser;
	}
}
