package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

import com.lcmg.rbloois.SystemException;
import com.lcmg.rbloois.util.Assert;


/**
 * Manages connection to the database.
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
	
	/**
	 * Configures the classname for the database driver and the url to the database.
	 * 
	 * @param driverName
	 * @param url
	 */
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
	
	/**
	 * Retrieves a connection from the DriverManager with the given url, user and password. Autocommit is set to true.
	 * 
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 */
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
	
	/**
	 * Gets a connection for the given user. If a connection for the given user is not available, this method will
	 * request a password by calling {@link Patcher#callBack}. A connection for a specific user is saved for later
	 * retrieval. A future call to this method with the same user will return the same connection. It uses the database
	 * configuration set by {@link #setConnection(String, String)}.
	 * 
	 * @param user
	 * @return
	 */
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
	
	/**
	 * Gets a connection for the default user.
	 * 
	 * @return
	 * @see #getConnection(String)
	 */
	static protected Connection getConnection()
	{
		Assert.notEmpty( defaultUser, "User must not be empty" );
		return getConnection( defaultUser );
	}

	/**
	 * Sets the default user.
	 * 
	 * @param defaultUser
	 */
	static protected void setDefaultUser( String defaultUser )
	{
		Assert.notEmpty( defaultUser, "User must not be empty" );
		Database.defaultUser = defaultUser;
	}
}
