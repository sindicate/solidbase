package solidbase.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import solidbase.core.SystemException;

/**
 * A datasource that gets its connections from the DriverManager.
 *
 * @author René M. de Bloois
 */
public class DriverDataSource implements javax.sql.DataSource
{
	/**
	 * The class name of the driver to be used to access the database.
	 */
	protected String driverClassName;

	/**
	 * The URL of the database.
	 */
	protected String url;

	/**
	 * The default user name to use for this database.
	 */
	protected String username;

	/**
	 * The password belonging to the default user.
	 */
	protected String password;


	/**
	 * Constructor.
	 *
	 * @param driverClassName The database driver class name.
	 * @param url The database URL.
	 * @param username The default user name.
	 * @param password The password for the default user.
	 */
	public DriverDataSource( String driverClassName, String url, String username, String password )
	{
		Assert.notNull( driverClassName );
		Assert.notNull( url );

		this.driverClassName = driverClassName;
		this.url = url;
		this.username = username;
		this.password = password;

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
	 * Returns a new connection using the default user.
	 */
	public Connection getConnection() throws SQLException
	{
		try
		{
			Connection connection = DriverManager.getConnection( this.url, this.username, this.password );
			connection.setAutoCommit( false );
			return connection;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Returns a new connection using the given user name and password.
	 *
	 * @param username The user name to connect with.
	 * @param password The password of the user.
	 */
	public Connection getConnection( String username, String password ) throws SQLException
	{
		try
		{
			Connection connection = DriverManager.getConnection( this.url, username, password );
			connection.setAutoCommit( false );
			return connection;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	public PrintWriter getLogWriter() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setLogWriter( PrintWriter out ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setLoginTimeout( int seconds ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getLoginTimeout() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public < T > T unwrap( Class< T > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isWrapperFor( Class< ? > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}
}
