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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;


/**
 * The Maven plugin for SolidBase.
 * 
 * @author Ruud de Jong
 * @author René de Bloois
 */
public class UpgradeMojo extends AbstractMojo
{
	/**
	 * The Maven Project Object
	 */
	private MavenProject project;

	/**
	 * Database driver class.
	 */
	private String driver;

	/**
	 * Database URL.
	 */
	private String url;

	/**
	 * Database username.
	 */
	private String username;

	/**
	 * Database password.
	 */
	private String password;

	/**
	 * File containing the upgrade.
	 */
	private String upgradefile;

	/**
	 * Target to upgrade the database to.
	 */
	private String target;

	/**
	 * Allow downgrades to reach the target.
	 */
	private boolean downgradeallowed;

	/**
	 * An array of secondary connections.
	 */
	private Secondary[] connections;

	/**
	 * Constructor.
	 */
	public UpgradeMojo()
	{
		super();
	}

	/**
	 * Validate the configuration of the plugin.
	 * 
	 * @throws MojoFailureException Whenever a configuration item is missing.
	 */
	protected void validate() throws MojoFailureException
	{
		// The rest is checked by Maven itself

		if( this.connections != null )
			for( Secondary secondary : this.connections )
			{
				if( secondary.getName() == null )
					throw new MojoFailureException( "The 'name' attribute is mandatory for a 'secondary' element" );
				if( secondary.getUsername() == null )
					throw new MojoFailureException( "The 'user' attribute is mandatory for a 'secondary' element" );
				if( secondary.getName().equals( "default" ) )
					throw new MojoFailureException( "The secondary name 'default' is reserved" );
			}
	}

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
			Patcher patcher = new Patcher( progress, new Database( this.driver, this.url, this.username, this.password == null ? "" : this.password, progress ) );

			if( this.connections != null )
				for( Secondary secondary : this.connections )
					patcher.addDatabase( secondary.getName(),
							new Database( secondary.getDriver() == null ? this.driver : secondary.getDriver(),
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
		catch( SQLExecutionException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
	}
}
