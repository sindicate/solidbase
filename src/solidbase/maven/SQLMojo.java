/*--
 * Copyright 2010 Ren� M. de Bloois
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

import solidbase.core.FatalException;
import solidbase.core.Runner;
import solidstack.io.ResourceFactory;


/**
 * The Maven plugin for SolidBase.
 *
 * @author Ren� de Bloois
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

		Runner runner = new Runner();
		runner.setProgressListener( new Progress( getLog() ) );
		runner.setConnectionAttributes( "default", this.driver, this.url, this.username, this.password == null ? "" : this.password );
		if( this.connections != null )
			for( Secondary connection : this.connections )
				runner.setConnectionAttributes(
						connection.getName(),
						connection.getDriver(),
						connection.getUrl(),
						connection.getUsername(),
						connection.getPassword() == null ? "" : connection.getPassword()
						);
		runner.setSQLFile( ResourceFactory.getResource( this.project.getBasedir() ).resolve( this.sqlfile ) );
		try
		{
			runner.executeSQL();
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
	}
}
