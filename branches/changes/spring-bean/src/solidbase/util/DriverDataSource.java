package solidbase.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import solidbase.core.SystemException;

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
	 * @param driverClassName
	 * @param url
	 * @param username
	 * @param password
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
