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

package solidbase.maven;

import org.apache.maven.plugin.MojoFailureException;
import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.SQLProcessor;
import solidbase.core.Util;


/**
 * The Maven plugin for SolidBase.
 * 
 * @author René de Bloois
 */
public class SQLMojo extends DBMojo
{
	/**
	 * File containing the upgrade.
	 */
	protected String sqlfile;

	public void execute() throws MojoFailureException
	{
		validate();

		Progress progress = new Progress( getLog() );

		String[] info = Version.getInfo();
		getLog().info( info[ 0 ] );
		getLog().info( info[ 1 ] );
		getLog().info( "" );

		try
		{
			SQLProcessor processor = new SQLProcessor( progress, new Database( "default", this.driver, this.url, this.username, this.password == null ? "" : this.password, progress ) );

			if( this.connections != null )
				for( Secondary secondary : this.connections )
					processor.addDatabase(
							new Database( secondary.getName(), secondary.getDriver() == null ? this.driver : secondary.getDriver(),
									secondary.getUrl() == null ? this.url : secondary.getUrl(),
											secondary.getUsername(), secondary.getPassword() == null ? "" : secondary.getPassword(), progress ) );

			processor.setCommandSource( Util.openSQLFile( this.project.getBasedir(), this.sqlfile, progress ) );
			try
			{
				progress.info( "Connecting to database..." );
				processor.execute();
				progress.info( "" );
			}
			finally
			{
				processor.end();
			}
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
	}
}
