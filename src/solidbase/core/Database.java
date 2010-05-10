/*--
 * Copyright 2006 René M. de Bloois
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


/**
 * Manages connections to the database.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:16:37 PM
 */
public class Database
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
	 * A map of connections indexed by user name.
	 */
	protected HashMap< String, Connection > connections = new HashMap< String, Connection >();

	/**
	 * The default user name to use for this database.
	 */
	protected String defaultUser;

	/**
	 * The password belonging to the default user.
	 */
	protected String defaultPassword;

	/**
	 * The current user.
	 */
	private String currentUser;

	/**
	 * The progress listener.
	 */
	protected ProgressListener callBack;

	/**
	 * Constructor for a specific database and default user. This object will manage multiple connections to this database.
	 *
	 * @param driverClassName Driver class name for the database.
	 * @param url URL for the database.
	 * @param defaultUser The default user name.
	 * @param defaultPassword The password belonging to the default user.
	 * @param callBack The progress listener.
	 */
	public Database( String driverClassName, String url, String defaultUser, String defaultPassword, ProgressListener callBack )
	{
		Assert.notNull( driverClassName );
		Assert.notNull( url );

		this.driverName = driverClassName;
		this.url = url;
		this.defaultUser = defaultUser;
		this.defaultPassword = defaultPassword;
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
	 * Resets the current user and initializes the connection if a password is known.
	 */
	public void init()
	{
		this.currentUser = this.defaultUser;
		if( this.defaultPassword != null )
			initConnection( this.defaultUser, this.defaultPassword ); // This prevents the password being requested from the user.
	}

	/**
	 * Returns a new connection for the given URL, user name and password. The connection has auto commit disabled.
	 *
	 * @param url URL for the database.
	 * @param user The user name.
	 * @param password The password of the user.
	 * @return A new connection with auto commit disabled.
	 */
	static protected Connection getConnection( String url, String user, String password )
	{
		try
		{
			Connection connection = DriverManager.getConnection( url, user, password );
			connection.setAutoCommit( false );
			return connection;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Sets up a connection for the given user name. Does nothing if the connection is already initiated.
	 *
	 * @param user The user name.
	 * @param password The password.
	 */
	protected void initConnection( String user, String password )
	{
		Connection connection = this.connections.get( user );
		if( connection == null )
		{
			connection = getConnection( this.url, user, password );
			this.connections.put( user, connection );
		}
	}

	/**
	 * Returns a connection for the given user name. Connections are cached per user. If a connection for the given user
	 * is not found in the cache, a password will be requested by calling
	 * {@link ProgressListener#requestPassword(String)} referenced by {@link PatchProcessor#progress}.
	 * 
	 * @param user The user name.
	 * @return The connection for the given user name.
	 */
	protected Connection getConnection( String user )
	{
		Connection connection = this.connections.get( user );
		if( connection == null )
		{
			String password = this.callBack.requestPassword( user );
			connection = getConnection( this.url, user, password );
			this.connections.put( user, connection );
		}
		return connection;
	}

	/**
	 * Returns a connection for the current user. Connections are cached per user. If a connection for the current user
	 * is not found in the cache, a password will be requested by calling
	 * {@link ProgressListener#requestPassword(String)} referenced by {@link PatchProcessor#progress}.
	 * 
	 * @return The connection for the current user.
	 * @see #getConnection(String)
	 */
	public Connection getConnection()
	{
		Assert.notEmpty( this.currentUser, "Current user must not be empty" );
		return getConnection( this.currentUser );
	}

	/**
	 * Sets the current user.
	 *
	 * @param user The user name that should made current.
	 */
	protected void setCurrentUser( String user )
	{
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
	 * Close all open connections that are maintained by this instance of {@link Database}.
	 */
	protected void closeConnections()
	{
		for( Connection connection : this.connections.values() )
		{
			try
			{
				connection.close();
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}

		this.connections.clear();
	}
}
