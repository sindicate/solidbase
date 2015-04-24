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
import solidbase.core.PatchProcessor;


/**
 * The Maven plugin for SolidBase.
 * 
 * @author Ruud de Jong
 * @author René de Bloois
 */
public class UpgradeMojo extends DBMojo
{
	/**
	 * File containing the upgrade.
	 */
	protected String upgradefile;

	/**
	 * Target to upgrade the database to.
	 */
	protected String target;

	/**
	 * Allow downgrades to reach the target.
	 */
	private boolean downgradeallowed;

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
			PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", this.driver, this.url, this.username, this.password == null ? "" : this.password, progress ) );

			if( this.connections != null )
				for( Secondary secondary : this.connections )
					patcher.addDatabase(
							new Database( secondary.getName(), secondary.getDriver() == null ? this.driver : secondary.getDriver(),
									secondary.getUrl() == null ? this.url : secondary.getUrl(),
											secondary.getUsername(), secondary.getPassword() == null ? "" : secondary.getPassword(), progress ) );

			patcher.init( this.project.getBasedir(), this.upgradefile );
			try
			{
				progress.info( "Connecting to database..." );
				progress.info( patcher.getVersionStatement() );
				patcher.patch( this.target, this.downgradeallowed ); // TODO Print this target
				progress.info( "" );
				progress.info( patcher.getVersionStatement() );
			}
			finally
			{
				patcher.end();
			}
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
	}
}
