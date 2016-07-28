/*--
 * Copyright 2006 Ren� M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import solidbase.util.Assert;


/**
 * Manages connections to the database.
 *
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:16:37 PM
 */
public class Database
{
	/**
	 * Name for this database.
	 */
	protected String name;

	/**
	 * The class name of the driver to be used to access the database. Ignored when {@link #dataSource} is set.
	 */
	protected String driverName;

	/**
	 * The URL of the database. Ignored when {@link #dataSource} is set.
	 */
	protected String url;

	/**
	 * The data source. Overrules {@link #driverName} and {@link #url}.
	 */
	protected DataSource dataSource;

	/**
	 * A map of connections indexed by user name.
	 */
	protected Map< String, Connection > connections = new HashMap< String, Connection >();

	protected Connection versionTablesConnection;

	/**
	 * The default user name to use for this database. When using a {@link #dataSource} this can be left blank.
	 * Connection are then retrieved from the datasource without specifying a user name.
	 */
	protected String defaultUser;

	/**
	 * The password cache.
	 */
	protected Map< String, String > passwords = new HashMap< String, String >();

	/**
	 * The current user. If {@link #defaultUser} is not set, this should stay null also.
	 */
	private String currentUser;

	/**
	 * The progress listener.
	 */
	protected ProgressListener callBack;

	/**
	 * The connection listener that listens for connection events.
	 */
	protected ConnectionListener connectionListener;

	/**
	 * Constructor for a named database that manages connections for multiple users.
	 *
	 * @param name The name of the database.
	 * @param driverClassName Driver class name for the database.
	 * @param url URL for the database.
	 * @param defaultUser The default user name.
	 * @param defaultPassword The password belonging to the default user.
	 * @param callBack The progress listener.
	 */
	public Database( String name, String driverClassName, String url, String defaultUser, String defaultPassword, ProgressListener callBack )
	{
		Assert.notNull( name );
		Assert.notNull( driverClassName );
		Assert.notNull( url );
		Assert.notNull( defaultUser );

		this.name = name;
		this.driverName = driverClassName;
		this.url = url;
		this.defaultUser = defaultUser;
		if( defaultPassword != null )
			this.passwords.put( defaultUser, defaultPassword );
		this.callBack = callBack;

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
	 * Constructor for a named database that manages connections for multiple users.
	 *
	 * @param name The name of the database.
	 * @param dataSource The data source providing connections to the database.
	 * @param defaultUser The default user name.
	 * @param defaultPassword The password belonging to the default user.
	 * @param callBack The progress listener.
	 */
	public Database( String name, DataSource dataSource, String defaultUser, String defaultPassword, ProgressListener callBack )
	{
		Assert.notNull( name );
		Assert.notNull( dataSource );

		this.name = name;
		this.dataSource = dataSource;
		this.callBack = callBack;
		this.defaultUser = defaultUser;
		if( defaultPassword != null )
			this.passwords.put( defaultUser, defaultPassword );
	}

	/**
	 * Constructor for a named database. You can't use multiple users with this database.
	 *
	 * @param name The name of the database.
	 * @param dataSource The data source providing connections to the database.
	 * @param callBack The progress listener.
	 */
	public Database( String name, DataSource dataSource, ProgressListener callBack )
	{
		this( name, dataSource, null, null, callBack );
	}

	/**
	 * Resets the current user and initializes the connection if a password is known.
	 */
	public void init()
	{
		this.currentUser = this.defaultUser;
	}

	/**
	 * Returns the name of this database.
	 *
	 * @return The name of this database.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the current user.
	 *
	 * @return The current user.
	 */
	public String getCurrentUser()
	{
		return this.currentUser;
	}

	/**
	 * Sets the connection listener that listens to connection events.
	 *
	 * @param connectionListener The connection listener.
	 */
	public void setConnectionListener( ConnectionListener connectionListener )
	{
		this.connectionListener = connectionListener;
	}

	/**
	 * Returns a connection for the current user. Connections are cached per user. If a connection for the current user
	 * is not found in the cache, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The connection for the current user.
	 */
	public Connection getConnection()
	{
		return getConnection( this.currentUser );
	}

	/**
	 * Returns a new connection for the current user. Passwords are remembered. If a password for the current user
	 * is not known, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The new connection for the current user.
	 */
	public Connection newConnection()
	{
		return newConnection( this.currentUser );
	}

	/**
	 * Returns a connection for the default user. Connections are cached per user. If the password for the default user
	 * has not been specified, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The connection for the default user.
	 */
	public Connection getVersionTablesConnection()
	{
		if( this.versionTablesConnection == null )
		{
			this.versionTablesConnection = newConnection( this.defaultUser );
			try
			{
				this.versionTablesConnection.setAutoCommit( false );
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
		return this.versionTablesConnection;
	}

	/**
	 * Returns a connection for the given user. Connections are cached per user. If a connection for the current user
	 * is not found in the cache, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @param user The user to get a connection for.
	 * @return The connection for the given user.
	 */
	public Connection getConnection( String user )
	{
		// State checks
		if( this.dataSource == null )
		{
			Assert.notEmpty( this.defaultUser );
			Assert.notEmpty( user );
			Assert.notNull( this.url );
		}
		if( this.defaultUser == null )
		{
			Assert.notNull( this.dataSource );
			Assert.isNull( user );
		}
		else
			Assert.notNull( user );

		// Look in cache
		Connection connection = this.connections.get( user );
		if( connection == null )
		{
			connection = newConnection( user );

			// Cache connection
			this.connections.put( user, connection ); // Put first, otherwise endless loop

			// Call listener
			// TODO Should this be moved to newConnection()?
			if( this.connectionListener != null )
				this.connectionListener.connected( this ); // TODO Check that auto commit is still off.
		}

		return connection;
	}

	/**
	 * Returns a new connection for the given user. Passwords are remembered. If a password for the given user
	 * is not known, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @param user The user name for the connection.
	 * @return The new connection for the current user.
	 */
	public Connection newConnection( String user )
	{
		// Retrieve password when user is specified
		String password = null;
		boolean cacheWhenSuccesful = false;
		if( user != null )
		{
			password = this.passwords.get( user );
			if( password == null )
			{
				password = this.callBack.requestPassword( user );
				cacheWhenSuccesful = true;
			}
		}

		try
		{
			Connection connection;

			// Get connection
			if( this.dataSource != null )
				if( user != null )
					connection = this.dataSource.getConnection( user, password );
				else
					connection = this.dataSource.getConnection();
			else
				connection = DriverManager.getConnection( this.url, user, password );

//			System.out.println( "DatabaseProductName:" + connection.getMetaData().getDatabaseProductName() );
//			System.out.println( "DatabaseProductVersion:" + connection.getMetaData().getDatabaseProductVersion() );
//			System.out.println( "DatabaseMajorVersion:" + connection.getMetaData().getDatabaseMajorVersion() );
//			System.out.println( "DatabaseMinorVersion:" + connection.getMetaData().getDatabaseMinorVersion() );

			if( cacheWhenSuccesful )
				this.passwords.put( user, password );

			return connection;
		}
		catch( SQLException e )
		{
			throw new FatalException( e );
		}
	}

	/**
	 * Sets the current user.
	 *
	 * @param user The user name that should made current.
	 */
	protected void setCurrentUser( String user )
	{
		if( this.defaultUser == null )
			throw new UnsupportedOperationException( "Can't change the user when the default user is not set." );
		Assert.notEmpty( user, "User must not be empty" );
		this.currentUser = user;
	}

	/**
	 * Return the default user.
	 *
	 * @return The name of the default user.
	 */
	public String getDefaultUser()
	{
		return this.defaultUser;
	}

	/**
	 * The current user becomes the default user.
	 */
	public void resetUser()
	{
		if( this.defaultUser != null )
			this.currentUser = this.defaultUser;
	}

	/**
	 * Close all open connections that are maintained by this instance of {@link Database}.
	 */
	protected void closeConnections()
	{
		for( Connection connection : this.connections.values() )
			try
			{
//				connection.rollback(); // TODO Derby 10.6 does not like it when connection is closed during an open transaction
				connection.close();
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}

		this.connections.clear();
	}
}
