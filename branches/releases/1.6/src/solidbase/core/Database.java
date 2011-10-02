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
import java.util.Map;

import solidbase.util.Assert;


/**
 * Manages connections to the database.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:16:37 PM
 */
public class Database
{
	/**
	 * Name for this database.
	 */
	protected String name;

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
	protected Map< String, String > passwords = new HashMap< String, String >();

	/**
	 * The current user.
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
	 * Constructor for a specific database and default user. This object will manage multiple connections to this database.
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
		Assert.notNull( driverClassName );
		Assert.notNull( url );

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
	 * Returns a connection for the current user. Connections are cached per user. If a connection for the current user
	 * is not found in the cache, a password will be requested by calling
	 * {@link ProgressListener#requestPassword(String)} referenced by {@link PatchProcessor#progress}.
	 * 
	 * @return The connection for the current user.
	 */
	public Connection getConnection()
	{
		Assert.notEmpty( this.currentUser, "Current user must not be empty" );
		Connection connection = this.connections.get( this.currentUser );
		if( connection == null )
		{
			String password = this.passwords.get( this.currentUser );
			if( password == null )
				password = this.callBack.requestPassword( this.currentUser );
			connection = getConnection( this.url, this.currentUser, password );
			this.connections.put( this.currentUser, connection ); // Put first, otherwise endless loop
			if( this.connectionListener != null )
				this.connectionListener.connected( this ); // TODO Check that auto commit is still off.
		}
		return connection;
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
