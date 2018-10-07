/*--
 * Copyright 2011 René M. de Bloois
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import solidbase.Version;
import solidbase.util.SynchronizedProtectedWorkerThread;
import solidstack.io.Resource;
import solidstack.script.scopes.MapScope;


/**
 * The runner contains the logic to execute upgrade files and SQL files and is used by the Ant tasks and Maven plugins.
 *
 * @author René M. de Bloois
 */
public class Runner
{
	/**
	 * The progress listener.
	 */
	protected ProgressListener listener;

	/**
	 * The named database connections.
	 */
	protected Map<String, ConnectionAttributes> connections = new HashMap<>();

	/**
	 * SQL files to execute.
	 */
	protected List<Resource> sqlFiles;

	/**
	 * Upgrade file to execute.
	 */
	protected Resource upgradeFile;

	/**
	 * The target to upgrade to.
	 */
	protected String upgradeTarget;

	/**
	 * Is a downgrade allowed during database upgrade?
	 */
	protected boolean downgradeAllowed;

	/**
	 * Where to send output.
	 */
	protected Resource outputFile;

	/**
	 * The parameters.
	 */
	protected Map<String, Object> parameters = new HashMap<>();

	private Duration retryInterval;

	private Duration retryDuration;

	/**
	 * Sets the progress listener.
	 *
	 * @param listener The progress listener.
	 */
	public void setProgressListener( ProgressListener listener ) {
		this.listener = listener;
	}

	/**
	 * Sets a connection to use.
	 *
	 * @param name The name of the connection.
	 * @param driver The driver class name to connect with.
	 * @param url The URL to connect with.
	 * @param username The user name to connect with.
	 * @param password The password of the user.
	 */
	public void setConnectionAttributes( String name, String driver, String url, String username, String password ) {
		connections.put( name, new ConnectionAttributes( name, driver, url, username, password ) );
	}

	/**
	 * Sets a connection to use.
	 *
	 * @param name The name of the connection.
	 * @param dataSource The data source to connect with.
	 * @param username The user name to connect with.
	 * @param password The password of the user.
	 */
	public void setConnectionAttributes( String name, DataSource dataSource, String username, String password ) {
		connections.put( name, new ConnectionAttributes( name, dataSource, username, password ) );
	}

	/**
	 * Set SQL files to execute.
	 *
	 * @param sqlFiles SQL files to execute.
	 */
	public void setSQLFiles( List<Resource> sqlFiles ) {
		this.sqlFiles = sqlFiles;
	}

	/**
	 * Set SQL file to execute.
	 *
	 * @param sqlFile SQL file to execute.
	 */
	public void setSQLFile( Resource sqlFile ) {
		sqlFiles = new ArrayList<>();
		sqlFiles.add( sqlFile );
	}

	/**
	 * Set the upgrade file.
	 *
	 * @param upgradeFile The upgrade file.
	 */
	public void setUpgradeFile( Resource upgradeFile ) {
		this.upgradeFile = upgradeFile;
	}

	/**
	 * Set the upgrade target.
	 *
	 * @param upgradeTarget The upgrade target.
	 */
	public void setUpgradeTarget( String upgradeTarget ) {
		this.upgradeTarget = upgradeTarget;
	}

	/**
	 * Set if a downgrade is allowed during upgrade.
	 *
	 * @param downgradeallowed Downgrade allowed during upgrade?
	 */
	public void setDowngradeAllowed( boolean downgradeallowed ) {
		downgradeAllowed = downgradeallowed;
	}

	/**
	 * Sets where to send output to.
	 *
	 * @param outputFile Where to send output to.
	 */
	public void setOutputFile( Resource outputFile ) {
		this.outputFile = outputFile;
	}

	/**
	 * Adds a parameter.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 */
	public void addParameter( String name, String value ) {
		parameters.put( name, value );
	}

	public void setRetryInterval(Duration interval) {
		retryInterval = interval;
	}

	public void setRetryDuration(Duration duration) {
		retryDuration = duration;
	}

	/**
	 * Execute the SQL files.
	 */
	public void executeSQL() {
		if( listener == null ) {
			throw new IllegalStateException( "ProgressListener not set" );
		}

		listener.println( Version.getInfo() );
		listener.println( "" );

		DatabaseContext databases = getDatabases();
		connect(databases.getDatabases());

		SQLProcessor processor = new SQLProcessor( listener );

		boolean complete = false;
		try {
			for( Resource resource : sqlFiles ) {
				// TODO Different levels of contexts and scopes, maybe merge them?
				SQLContext context = new SQLContext( Factory.openSQLFile( resource, listener ).getSource() );
				context.setDatabases( databases );
				context.swapScope( new MapScope( parameters, context.getScope() ) );
				processor.setContext( context );
				try {
					processor.process(); // TODO Why not a bigger try finally?
				} finally {
					processor.end();
				}
			}

			complete = true;

		} finally {
			if( complete ) {
				listener.sqlExecutionComplete();
			} else {
				listener.sqlExecutionAborted();
			}

			PluginManager.terminateListeners();
		}

		listener.println( "" );
	}

	/**
	 * Upgrade the database. This method protects itself against SIGINT (Ctrl-C).
	 */
	// TODO executeSQL() prints a newline at the end, but upgrade() does not
	public void upgrade() {
		if( listener == null ) {
			throw new IllegalStateException( "ProgressListener not set" );
		}

		listener.println( Version.getInfo() );
		listener.println( "" );

		DatabaseContext databases = getDatabases();
		connect(databases.getDatabases());

		final UpgradeProcessor processor = new UpgradeProcessor( listener );
		processor.setUpgradeFile( Factory.openUpgradeFile( upgradeFile, listener ) );
		processor.setDatabases( databases );
		processor.setParameters( parameters );

		final ProgressListener listener = this.listener;
		final String upgradeTarget = this.upgradeTarget;
		final boolean downgradeAllowed = this.downgradeAllowed;

		SynchronizedProtectedWorkerThread worker = new SynchronizedProtectedWorkerThread( "UpgradeThread" ) {
			@Override
			public void work() {
				boolean complete = false;
				try {
					processor.init();
					listener.println( processor.getVersionStatement() );
					processor.upgrade( upgradeTarget, downgradeAllowed ); // TODO Print this target
					listener.println( "" );
					listener.println( processor.getVersionStatement() );

					complete = true;

				} finally {
					if( complete ) {
						listener.upgradeComplete();
					} else {
						listener.upgradeAborted();
					}

					processor.end();
					PluginManager.terminateListeners();
				}
			}
		};

		worker.start();
	}

	private DatabaseContext getDatabases() {
		ConnectionAttributes def = connections.get( "default" );
		if( def == null ) {
			throw new IllegalArgumentException( "Missing 'default' connection." );
		}

		DatabaseContext databases = new DatabaseContext();
		for( ConnectionAttributes connection : connections.values() ) {
			DataSource dataSource = connection.getDatasource();
			String driver = connection.getDriver();
			if( driver == null && dataSource == null ) {
				driver = def.getDriver();
				dataSource = def.getDatasource();
			}
			String url = connection.getUrl();
			if( url == null ) {
				url = def.getUrl();
			}

			if( dataSource != null ) {
				databases.addDatabase( new Database( connection.getName(), dataSource, connection.getUsername(), connection.getPassword(), listener ) );
			} else {
				databases.addDatabase( new Database( connection.getName(), driver, url, connection.getUsername(), connection.getPassword(), listener ) );
			}
		}
		return databases;
	}

	public void connect(Collection<Database> databases) {
		boolean success = false;
		LocalDateTime started = LocalDateTime.now();

		do {
			try {
				for( Database db : databases ) {
					listener.println( "Connecting to database " + db.getName() + " (" + db.getUrl() + ")..." );
					db.getConnection();
				}
				success = true;

			} catch (Exception e) {
				if (retryInterval == null
						|| retryDuration != null
						&& started.plusSeconds( retryDuration.getSeconds() ).isBefore( LocalDateTime.now() )) {
//					e.printStackTrace( System.err ); TODO If debug?
					throw e;
				}
				listener.println( "Could not connect, retrying in " + retryInterval.getSeconds() + " seconds: " + e );
				try {
					Thread.sleep( retryInterval.toMillis() );
				} catch( InterruptedException ie ) {
					throw new SystemException( ie );
				}
			}

		} while (!success);
	}

	/**
	 * Dump the database log to an XML file.
	 */
	// TODO Replace with DUMP JSON
	public void logToXML() {
		if( listener == null ) {
			throw new IllegalStateException( "ProgressListener not set" );
		}

		listener.println( Version.getInfo() );
		listener.println( "" );

		UpgradeProcessor processor = new UpgradeProcessor( listener );

		ConnectionAttributes def = connections.get( "default" );
		if( def == null ) {
			throw new IllegalArgumentException( "Missing 'default' connection." );
		}

		DatabaseContext databases = new DatabaseContext();
		for( ConnectionAttributes connection : connections.values() ) {
			databases.addDatabase( new Database(
					connection.getName(),
					connection.getDriver() == null ? def.driver : connection.getDriver(),
					connection.getUrl() == null ? def.url : connection.getUrl(),
					connection.getUsername(),
					connection.getPassword(),
					listener ) );
		}

		processor.setUpgradeFile( Factory.openUpgradeFile( upgradeFile, listener ) );
		processor.setDatabases( databases );
		try {
			processor.init();
			processor.logToXML( outputFile );
		} finally {
			processor.end();
		}
	}

}
