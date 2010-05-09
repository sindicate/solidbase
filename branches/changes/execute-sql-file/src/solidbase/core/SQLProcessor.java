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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;


/**
 * This class is the coordinator. It reads commands from the {@link SQLFile}. It calls the {@link CommandListener}s,
 * calls the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 * 
 * @author René M. de Bloois
 * @since May 2010
 */
public class SQLProcessor extends CommandProcessor
{
	/**
	 * The sql file being executed.
	 */
	protected SQLFile sqlFile;

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 */
	public SQLProcessor( ProgressListener listener )
	{
		super( listener );
	}

	/**
	 * Construct a new instance of the sql executer.
	 * 
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public SQLProcessor( ProgressListener listener, Database database )
	{
		super( listener, database );
	}

	/**
	 * Initialize the sql executer.
	 * 
	 * @param baseDir The base folder from where to look for the upgrade file (optional).
	 * @param sqlFileName The name of the sql file.
	 */
	// TODO Remove this init, should be in the constructor
	public void init( File baseDir, String sqlFileName )
	{
		openSQLFile( baseDir, sqlFileName );
	}

	/**
	 * Initialize the sql executer.
	 * 
	 * @param sqlFileName The name of the sql file.
	 */
	// TODO Remove this init, should be in the constructor
	public void init( String sqlFileName )
	{
		init( null, sqlFileName );
	}

	/**
	 * Open the specified sql file.
	 * 
	 * @param fileName The name and path of the sql file.
	 */
	protected void openSQLFile( String fileName )
	{
		openSQLFile( null, fileName );
	}

	/**
	 * Open the specified sql file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the sql file.
	 */
	protected void openSQLFile( File baseDir, String fileName )
	{
		Assert.notNull( fileName );

		try
		{
			RandomAccessLineReader ralr;
			// TODO Should we remove this "/"?
			URL url = SQLProcessor.class.getResource( "/" + fileName ); // In the classpath
			if( url != null )
			{
				this.progress.openingSQLFile( url );
				ralr = new RandomAccessLineReader( url );
			}
			else
			{
				File file = new File( baseDir, fileName ); // In the current folder
				this.progress.openingSQLFile( file );
				ralr = new RandomAccessLineReader( file );
			}

			this.sqlFile = new SQLFile( ralr );

			this.progress.openedSQLFile( this.sqlFile );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Close the upgrade file.
	 */
	public void closeSQLFile()
	{
		if( this.sqlFile != null )
			this.sqlFile.close();
		this.sqlFile = null;
	}

	/**
	 * Execute the sql file.
	 * 
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void execute() throws SQLExecutionException
	{
		Command command = this.sqlFile.readStatement();
		while( command != null )
		{
			executeWithListeners( command );
			command = this.sqlFile.readStatement();
		}
		this.progress.sqlExecutionComplete();
	}

	/**
	 * Closes open files and closes connections.
	 */
	@Override
	public void end()
	{
		super.end();
		closeSQLFile();
	}
}
