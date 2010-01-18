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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import solidbase.Main;
import solidbase.config.Configuration;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;


/**
 * @author Ruud de Jong
 * @goal upgrade
 * @phase process-resources
 */
public class UpgradeMojo extends AbstractMojo
{
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
	 * Database user.
	 * 
	 * @parameter expression="${user}
	 * @required
	 */
	private String user;

	/**
	 * Database password.
	 * 
	 * @parameter expression="${password}
	 * @required
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
	 * The Maven Project Object
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Target to upgrade the database to.
	 * 
	 * @parameter expression="${downgradeallowed}
	 */
	private boolean downgradeallowed;

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		validate();

		Progress progress = new Progress( getLog() );

		Configuration configuration = new Configuration( progress );

		getLog().info( "SolidBase v" + configuration.getVersion() );
		getLog().info( "(C) 2006-2010 René M. de Bloois" );
		getLog().info( "" );

		Patcher patcher = new Patcher( progress, new Database( this.driver, this.url, this.user, this.password, progress ) );
		try
		{
			progress.info( "Connecting to database..." );

			progress.info( Main.getCurrentVersion( patcher ) );

			patcher.openPatchFile( this.project.getBasedir(), this.upgradefile );
			try
			{
				if( this.target != null )
					patcher.patch( this.target, this.downgradeallowed ); // TODO Print this target
				else
					throw new UnsupportedOperationException();
				progress.info( "" );
				progress.info( Main.getCurrentVersion( patcher ) );
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

	private void validate() throws MojoExecutionException
	{
		if( this.driver == null )
			throw new MojoExecutionException( "The 'driver' attribute is mandatory." );
		if( this.url == null )
			throw new MojoExecutionException( "The 'url' attribute is mandatory." );
		if( this.user == null )
			throw new MojoExecutionException( "The 'user' attribute is mandatory." );
		if( this.password == null )
			throw new MojoExecutionException( "The 'password' attribute is mandatory." );
		if( this.upgradefile == null )
			throw new MojoExecutionException( "The 'upgradefile' attribute is mandatory." );
		if( this.target == null )
			throw new MojoExecutionException( "The 'target' attribute is mandatory." );
	}
}
