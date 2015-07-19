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
import solidstack.io.Resources;
import solidstack.script.ScriptException;


/**
 * The Maven plugin for SolidBase.
 *
 * @author Ren� de Bloois
 */
public class UpgradeMojo extends DBMojo
{
	/**
	 * File containing the upgrade.
	 */
	public String upgradefile;

	/**
	 * Target to upgrade the database to.
	 */
	public String target;

	/**
	 * Allow downgrades to reach the target.
	 */
	public boolean downgradeallowed;

	public void execute() throws MojoFailureException
	{
		if( this.skip )
		{
			getLog().info( "Skipped." );
			getLog().info( "" );
			return;
		}

		validate();

		Runner runner = prepareRunner();
		try
		{
			runner.upgrade();
		}
		catch( FatalException e )
		{
			throw new MojoFailureException( e.getMessage() );
		}
		catch( ScriptException e )
		{
			// TODO Or should ScriptException be wrapped in a FatalException?
			throw new MojoFailureException( e.getMessage() );
		}
	}

	@Override
	public Runner prepareRunner()
	{
		Runner runner = super.prepareRunner();

		runner.setUpgradeFile( Resources.getResource( this.project.getBasedir() ).resolve( this.upgradefile ) );
		runner.setUpgradeTarget( this.target );
		runner.setDowngradeAllowed( this.downgradeallowed );

		return runner;
	}
}
