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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.Version;
import solidbase.util.Resource;

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
	protected Map< String, ConnectionAttributes > connections = new HashMap< String, ConnectionAttributes >();

	/**
	 * SQL files to execute.
	 */
	protected List< Resource > sqlFiles;

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
	 * Sets the progress listener.
	 *
	 * @param listener The progress listener.
	 */
	public void setProgressListener( ProgressListener listener )
	{
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
	public void setConnectionAttributes( String name, String driver, String url, String username, String password )
	{
		this.connections.put( name, new ConnectionAttributes( name, driver, url, username, password ) );
	}

	/**
	 * Set SQL files to execute.
	 *
	 * @param sqlFiles SQL files to execute.
	 */
	public void setSQLFiles( List< Resource > sqlFiles )
	{
		this.sqlFiles = sqlFiles;
	}

	/**
	 * Set SQL file to execute.
	 *
	 * @param sqlFile SQL file to execute.
	 */
	public void setSQLFile( Resource sqlFile )
	{
		this.sqlFiles = new ArrayList< Resource >();
		this.sqlFiles.add( sqlFile );
	}

	/**
	 * Set the upgrade file.
	 *
	 * @param upgradeFile The upgrade file.
	 */
	public void setUpgradeFile( Resource upgradeFile )
	{
		this.upgradeFile = upgradeFile;
	}

	/**
	 * Set the upgrade target.
	 *
	 * @param upgradeTarget The upgrade target.
	 */
	public void setUpgradeTarget( String upgradeTarget )
	{
		this.upgradeTarget = upgradeTarget;
	}

	/**
	 * Set if a downgrade is allowed during upgrade.
	 *
	 * @param downgradeallowed Downgrade allowed during upgrade?
	 */
	public void setDowngradeAllowed( boolean downgradeallowed )
	{
		this.downgradeAllowed = downgradeallowed;
	}

	/**
	 * Sets where to send output to.
	 *
	 * @param outputFile Where to send output to.
	 */
	public void setOutputFile( Resource outputFile )
	{
		this.outputFile = outputFile;
	}

	/**
	 * Execute the SQL files.
	 */
	public void executeSQL()
	{
		if( this.listener == null )
			throw new IllegalStateException( "ProgressListener not set" );

		this.listener.println( Version.getInfo() );
		this.listener.println( "" );

		SQLProcessor processor = new SQLProcessor( this.listener );

		ConnectionAttributes def = this.connections.get( "default" );
		if( def == null )
			throw new IllegalArgumentException( "Missing 'default' connection." );

		DatabaseContext databases = new DatabaseContext();
		for( ConnectionAttributes connection : this.connections.values() )
			databases.addDatabase( new Database(
							connection.getName(),
							connection.getDriver() == null ? def.driver : connection.getDriver(),
							connection.getUrl() == null ? def.url : connection.getUrl(),
							connection.getUsername(),
							connection.getPassword(),
							this.listener
					)
			);

		boolean complete = false;
		try
		{
			boolean first = true;
			for( Resource resource : this.sqlFiles )
			{
				SQLContext context = new SQLContext( Factory.openSQLFile( resource, this.listener ).getSource() );
				context.setDatabases( databases );
				processor.setContext( context );
				if( first )
				{
					this.listener.println( "Connecting to database..." ); // TODO Let the database say that (for example the default connection)
					first = false;
				}
				processor.process();
			}
			complete = true;
		}
		finally
		{
			if( complete )
				this.listener.sqlExecutionComplete();
			else
				this.listener.sqlExecutionAborted();

			processor.end();
			PluginManager.terminateListeners();
		}

		this.listener.println( "" );
	}

	/**
	 * Upgrade the database.
	 */
	public void upgrade()
	{
		if( this.listener == null )
			throw new IllegalStateException( "ProgressListener not set" );

		this.listener.println( Version.getInfo() );
		this.listener.println( "" );

		UpgradeProcessor processor = new UpgradeProcessor( this.listener );

		ConnectionAttributes def = this.connections.get( "default" );
		if( def == null )
			throw new IllegalArgumentException( "Missing 'default' connection." );

		DatabaseContext databases = new DatabaseContext();
		for( ConnectionAttributes connection : this.connections.values() )
			databases.addDatabase(
					new Database(
							connection.getName(),
							connection.getDriver() == null ? def.driver : connection.getDriver(),
							connection.getUrl() == null ? def.url : connection.getUrl(),
							connection.getUsername(),
							connection.getPassword(),
							this.listener
					)
			);

		processor.setUpgradeFile( Factory.openUpgradeFile( this.upgradeFile, this.listener ) );
		processor.setDatabases( databases );
		boolean complete = false;
		try
		{
			processor.init();
			this.listener.println( "Connecting to database..." );
			this.listener.println( processor.getVersionStatement() );
			processor.upgrade( this.upgradeTarget, this.downgradeAllowed ); // TODO Print this target
			this.listener.println( "" );
			this.listener.println( processor.getVersionStatement() );

			complete = true;
		}
		finally
		{
			if( complete )
				this.listener.upgradeComplete();
			else
				this.listener.upgradeAborted();

			processor.end();
			PluginManager.terminateListeners();
		}
	}

	/**
	 * Dump the database log to an XML file.
	 */
	public void logToXML()
	{
		if( this.listener == null )
			throw new IllegalStateException( "ProgressListener not set" );

		this.listener.println( Version.getInfo() );
		this.listener.println( "" );

		UpgradeProcessor processor = new UpgradeProcessor( this.listener );

		ConnectionAttributes def = this.connections.get( "default" );
		if( def == null )
			throw new IllegalArgumentException( "Missing 'default' connection." );

		DatabaseContext databases = new DatabaseContext();
		for( ConnectionAttributes connection : this.connections.values() )
			databases.addDatabase(
					new Database(
							connection.getName(),
							connection.getDriver() == null ? def.driver : connection.getDriver(),
							connection.getUrl() == null ? def.url : connection.getUrl(),
							connection.getUsername(),
							connection.getPassword(),
							this.listener
					)
			);

		processor.setUpgradeFile( Factory.openUpgradeFile( this.upgradeFile, this.listener ) );
		processor.setDatabases( databases );
		try
		{
			processor.init();
			processor.logToXML( this.outputFile );
		}
		finally
		{
			processor.end();
		}
	}
}
