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

import static solidbase.util.Nulls.nonNull;

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
	protected Map<String, Connection> connections = new HashMap<>();

	/**
	 * The connection where the version meta tables can be found.
	 */
	protected Connection versionTablesConnection;

	/**
	 * The default user name to use for this database. When using a {@link #dataSource} this can be left blank.
	 * Connection are then retrieved from the datasource without specifying a user name.
	 */
	protected String defaultUser;

	/**
	 * The password cache.
	 */
	protected Map<String, String> passwords = new HashMap<>();

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
	public Database( String name, String driverClassName, String url, String defaultUser, String defaultPassword, ProgressListener callBack ) {
		this.name = nonNull( name );
		driverName = nonNull( driverClassName );
		this.url = nonNull( url );
		this.defaultUser = nonNull( defaultUser );
		if( defaultPassword != null ) {
			passwords.put( defaultUser, defaultPassword );
		}
		this.callBack = callBack;

		try {
			Class.forName( driverClassName );
		} catch( ClassNotFoundException e ) {
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
	public Database( String name, DataSource dataSource, String defaultUser, String defaultPassword, ProgressListener callBack ) {
		this.name = nonNull( name );
		this.dataSource = nonNull( dataSource );
		this.callBack = callBack;
		this.defaultUser = defaultUser;
		if( defaultPassword != null ) {
			passwords.put( defaultUser, defaultPassword );
		}
	}

	/**
	 * Constructor for a named database. You can't use multiple users with this database.
	 *
	 * @param name The name of the database.
	 * @param dataSource The data source providing connections to the database.
	 * @param callBack The progress listener.
	 */
	public Database( String name, DataSource dataSource, ProgressListener callBack ) {
		this( name, dataSource, null, null, callBack );
	}

	/**
	 * Resets the current user and initializes the connection if a password is known.
	 */
	public void init() {
		currentUser = defaultUser;
	}

	/**
	 * Returns the name of this database.
	 *
	 * @return The name of this database.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the current user.
	 *
	 * @return The current user.
	 */
	public String getCurrentUser() {
		return currentUser;
	}

	/**
	 * Sets the connection listener that listens to connection events.
	 *
	 * @param connectionListener The connection listener.
	 */
	public void setConnectionListener( ConnectionListener connectionListener ) {
		this.connectionListener = connectionListener;
	}

	/**
	 * Returns a connection for the current user. Connections are cached per user. If a connection for the current user
	 * is not found in the cache, a password will be requested by calling
	 * {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The connection for the current user.
	 */
	public Connection getConnection() {
		return getConnection( currentUser );
	}

	/**
	 * Returns a new connection for the current user. Passwords are remembered. If a password for the current user is
	 * not known, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The new connection for the current user.
	 */
	public Connection newConnection() {
		return newConnection( currentUser );
	}

	/**
	 * Returns a connection for the default user. Connections are cached per user. If the password for the default user
	 * has not been specified, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @return The connection for the default user.
	 */
	public Connection getVersionTablesConnection() {
		if( versionTablesConnection == null ) {
			versionTablesConnection = newConnection( defaultUser );
			try {
				versionTablesConnection.setAutoCommit( false );
			} catch( SQLException e ) {
				throw new SystemException( e );
			}
		}
		return versionTablesConnection;
	}

	/**
	 * Returns a connection for the given user. Connections are cached per user. If a connection for the current user is
	 * not found in the cache, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @param user The user to get a connection for.
	 * @return The connection for the given user.
	 */
	public Connection getConnection( String user ) {

		// Check the current state
		if( dataSource == null ) {
			Assert.notEmpty( defaultUser );
			Assert.notEmpty( user );
			nonNull( url );
		}
		if( defaultUser == null ) {
			nonNull( dataSource );
			Assert.isNull( user );
		} else {
			nonNull( user );
		}

		// Look in cache
		Connection connection = connections.get( user );
		if( connection == null ) {
			connection = newConnection( user );

			// Cache connection
			connections.put( user, connection ); // Put first, otherwise endless loop

			// Call listener
			// TODO Should this be moved to newConnection()?
			if( connectionListener != null ) {
				connectionListener.connected( this ); // TODO Check that auto commit is still off.
			}
		}

		return connection;
	}

	/**
	 * Returns a new connection for the given user. Passwords are remembered. If a password for the given user is not
	 * known, a password will be requested by calling {@link ProgressListener#requestPassword(String)}.
	 *
	 * @param user The user name for the connection.
	 * @return The new connection for the current user.
	 */
	@SuppressWarnings( "resource" )
	public Connection newConnection( String user ) {
		// Retrieve password when user is specified
		String password = null;
		boolean cacheWhenSuccesful = false;
		if( user != null ) {
			password = passwords.get( user );
			if( password == null ) {
				password = callBack.requestPassword( user );
				cacheWhenSuccesful = true;
			}
		}

		try {
			Connection connection;

			// Get connection
			if( dataSource != null ) {
				if( user != null ) {
					connection = dataSource.getConnection( user, password );
				} else {
					connection = dataSource.getConnection();
				}
			} else {
				connection = DriverManager.getConnection( url, user, password );
			}

//			System.out.println( "DatabaseProductName:" + connection.getMetaData().getDatabaseProductName() );
//			System.out.println( "DatabaseProductVersion:" + connection.getMetaData().getDatabaseProductVersion() );
//			System.out.println( "DatabaseMajorVersion:" + connection.getMetaData().getDatabaseMajorVersion() );
//			System.out.println( "DatabaseMinorVersion:" + connection.getMetaData().getDatabaseMinorVersion() );

			if( cacheWhenSuccesful ) {
				passwords.put( user, password );
			}

			return connection;
		} catch( SQLException e ) {
			throw new FatalException( e );
		}
	}

	/**
	 * Sets the current user.
	 *
	 * @param user The user name that should made current.
	 */
	protected void setCurrentUser( String user ) {
		if( defaultUser == null ) {
			throw new UnsupportedOperationException( "Can't change the user when the default user is not set." );
		}
		Assert.notEmpty( user, "User must not be empty" );
		currentUser = user;
	}

	/**
	 * Return the default user.
	 *
	 * @return The name of the default user.
	 */
	public String getDefaultUser() {
		return defaultUser;
	}

	/**
	 * The current user becomes the default user.
	 */
	public void resetUser() {
		if( defaultUser != null ) {
			currentUser = defaultUser;
		}
	}

	/**
	 * Close all open connections that are maintained by this instance of {@link Database}.
	 */
	protected void closeConnections() {
		if( versionTablesConnection != null ) {
			close( versionTablesConnection );
			versionTablesConnection = null;
		}

		for( Connection connection : connections.values() ) {
			close( connection );
		}
		connections.clear();
	}

	/**
	 * Closes the given connection.
	 *
	 * @param connection The connection to close.
	 */
	protected void close( Connection connection ) {
		try {
//			connection.rollback(); // TODO Derby 10.6 does not like it when connection is closed during an open transaction
			connection.close();
		} catch( SQLException e ) {
			throw new SystemException( e );
		}
	}

}
