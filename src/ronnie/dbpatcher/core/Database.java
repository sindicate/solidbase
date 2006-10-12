package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;


/**
 * Manages connections to the database.
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
	 * @param driverClassName The database driver class name.
	 * @param url The database connection url.
	 */
	static protected void setConnection( String driverClassName, String url )
	{
		Database.driverName = driverClassName;
		Database.url = url;
		try
		{
			Class.forName( driverClassName );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}
	}
	
	/**
	 * Gets a new connection from the DriverManager with the given url, user and password. The returned connection has autocommit set to on.
	 * 
	 * @param url The database connection url.
	 * @param user The connection user.
	 * @param password The password of the user.
	 * @return the connection.
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
	 * Gets a cached connection for the given user. If a connection for the given user is not found in the cache, this method will
	 * request a password by calling the method {@link ProgressListener#requestPassword(String)} of {@link Patcher#callBack}. The connection is cached for later use.
	 * It uses the database configuration set by {@link #setConnection(String, String)}.
	 * 
	 * @param user The user name.
	 * @return the connection
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
	 * @return the connection.
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
	 * @param user
	 */
	static protected void setDefaultUser( String user )
	{
		Assert.notEmpty( user, "User must not be empty" );
		Database.defaultUser = user;
	}
}
