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

import solidbase.core.FatalException;
import solidbase.core.Runner;
import solidstack.io.ResourceFactory;


/**
 * The Maven plugin for SolidBase.
 *
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
		runner.setUpgradeFile( ResourceFactory.getResource( this.project.getBasedir(), this.upgradefile ) );
		runner.setUpgradeTarget( this.target );
		runner.setDowngradeAllowed( this.downgradeallowed );
		try
		{
			runner.upgrade();
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
	}
}
