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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;


/**
 * @author Ruud de Jong
 * @author Ren� de Bloois
 * @goal upgrade
 * @phase process-resources
 */
public class UpgradeMojo extends AbstractMojo
{
	/**
	 * The Maven Project Object
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Database driver class.
	 * 
	 * @parameter expression="${driver}
	 * @required
	 */
	private String driver;

	/**
	 * Database URL.
	 * 
	 * @parameter expression="${url}
	 * @required
	 */
	private String url;

	/**
	 * Database username.
	 * 
	 * @parameter expression="${username}
	 * @required
	 */
	private String username;

	/**
	 * Database password.
	 * 
	 * @parameter expression="${password}
	 */
	private String password;

	/**
	 * File containing the upgrade.
	 * 
	 * @parameter expression="${upgradefile}
	 * @required
	 */
	private String upgradefile;

	/**
	 * Target to upgrade the database to.
	 * 
	 * @parameter expression="${target}
	 * @required
	 */
	private String target;

	/**
	 * Allow downgrades to reach the target.
	 * 
	 * @parameter expression="${downgradeallowed}"
	 */
	private boolean downgradeallowed;

	/**
	 * An array of secondary connections.
	 */
	private Secondary[] connections;

	/**
	 * Validate the configuration of the plugin.
	 * 
	 * @throws MojoExecutionException Whenever a configuration item is missing.
	 */
	protected void validate() throws MojoExecutionException
	{
		// The rest is checked by Maven itself

		if( this.connections != null )
			for( Secondary secondary : this.connections )
			{
				if( secondary.getName() == null )
					throw new MojoExecutionException( "The 'name' attribute is mandatory for a 'secondary' element" );
				if( secondary.getUsername() == null )
					throw new MojoExecutionException( "The 'user' attribute is mandatory for a 'secondary' element" );
				if( secondary.getName().equals( "default" ) )
					throw new MojoExecutionException( "The secondary name 'default' is reserved" );
			}
	}

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		validate();

		Progress progress = new Progress( getLog() );

		String[] info = Version.getInfo();
		getLog().info( info[ 0 ] );
		getLog().info( info[ 1 ] );
		getLog().info( "" );

		Patcher patcher = new Patcher( progress, new Database( this.driver, this.url, this.username, this.password == null ? "" : this.password, progress ) );
		try
		{
			if( this.connections != null )
				for( Secondary secondary : this.connections )
					patcher.addDatabase( secondary.getName(),
							new Database( secondary.getDriver() == null ? this.driver : secondary.getDriver(),
									secondary.getUrl() == null ? this.url : secondary.getUrl(),
											secondary.getUsername(), secondary.getPassword() == null ? "" : secondary.getPassword(), progress ) );

			progress.info( "Connecting to database..." );

			progress.info( patcher.getVersionStatement() );

			patcher.openPatchFile( this.project.getBasedir(), this.upgradefile );
			try
			{
				if( this.target != null )
					patcher.patch( this.target, this.downgradeallowed ); // TODO Print this target
				else
					throw new UnsupportedOperationException();
				progress.info( "" );
				progress.info( patcher.getVersionStatement() );
			}
			finally
			{
				patcher.closePatchFile();
			}
		}
		catch( SQLExecutionException e )
		{
			throw new MojoExecutionException( e.getMessage() );
		}
		catch( FatalException e )
		{
			throw new MojoExecutionException( e.getMessage() );
		}
		finally
		{
			patcher.end();
		}
	}
}
