package solidbase.core;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import solidbase.util.Assert;

public class TestDataSource implements javax.sql.DataSource
{
	/**
	 * The class name of the driver to be used to access the database.
	 */
	protected String driverName;

	/**
	 * The URL of the database.
	 */
	protected String url;

	/**
	 * The default user name to use for this database.
	 */
	protected String defaultUser;

	/**
	 * The password belonging to the default user.
	 */
	protected String defaultPassword;


	/**
	 * Constructor.
	 *
	 * @param driverClassName
	 * @param url
	 * @param defaultUser
	 * @param defaultPassword
	 */
	public TestDataSource( String driverClassName, String url, String defaultUser, String defaultPassword )
	{
		Assert.notNull( driverClassName );
		Assert.notNull( url );

		this.driverName = driverClassName;
		this.url = url;
		this.defaultUser = defaultUser;
		this.defaultPassword = defaultPassword;

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
			Connection connection = DriverManager.getConnection( this.url, this.defaultUser, this.defaultPassword );
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
