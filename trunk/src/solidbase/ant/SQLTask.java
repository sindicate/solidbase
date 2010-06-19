/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.SQLProcessor;


/**
 * The Sql Ant Task.
 * 
 * @author René M. de Bloois
 */
public class SQLTask extends DBTask
{
	/**
	 * Field to store the configured sql file.
	 */
	protected String sqlfile;

	/**
	 * Returns the configured sql file.
	 * 
	 * @return the configured sql file.
	 */
	public String getSqlfile()
	{
		return this.sqlfile;
	}

	/**
	 * Sets the sql file to configure.
	 * 
	 * @param sqlfile The sql file to configure.
	 */
	public void setSqlfile( String sqlfile )
	{
		this.sqlfile = sqlfile;
	}

	/**
	 * Validates the configuration of the Ant Task.
	 */
	@Override
	protected void validate()
	{
		super.validate();

		if( this.sqlfile == null )
			throw new BuildException( "The 'sqlfile' attribute is mandatory for the " + getTaskName() + " task" );
	}


	@Override
	public void execute()
	{
		validate();

		Project project = getProject();
		Progress progress = new Progress( project, this );

		String[] info = Version.getInfo();
		progress.info( info[ 0 ] );
		progress.info( info[ 1 ] );
		progress.info( "" );

		try
		{
			SQLProcessor executer = new SQLProcessor( progress, new Database( "default", this.driver, this.url, this.username, this.password, progress ) );

			for( Connection connection : this.connections )
				executer.addDatabase(
						new Database( connection.getName(), connection.getDriver() == null ? this.driver : connection.getDriver(),
								connection.getUrl() == null ? this.url : connection.getUrl(),
										connection.getUsername(), connection.getPassword(), progress ) );

			executer.init( project.getBaseDir(), this.sqlfile );
			try
			{
				progress.info( "Connecting to database..." );
				executer.execute();
				progress.info( "" );
			}
			finally
			{
				executer.end();
			}
		}
		catch( FatalException e )
		{
			throw new BuildException( e.getMessage() );
		}
	}
}
