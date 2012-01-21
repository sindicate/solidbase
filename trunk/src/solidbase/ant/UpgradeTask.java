/*--
 * Copyright 2009 René M. de Bloois
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

import solidbase.core.FatalException;
import solidbase.core.Runner;
import solidbase.io.ResourceFactory;


/**
 * The Upgrade Ant Task.
 *
 * @author René M. de Bloois
 */
public class UpgradeTask extends DBTask
{
	/**
	 * Field to store the configured upgrade file.
	 */
	protected String upgradefile;

	/**
	 * Field to store the configured target.
	 */
	protected String upgradeTarget;

	/**
	 * Field to store the configured downgrade allowed option.
	 */
	protected boolean downgradeallowed;

	/**
	 * Sets the user name to configure.
	 *
	 * @param username The user name to configure.
	 */
	@Deprecated
	public void setUser( String username )
	{
		this.username = username;
	}

	/**
	 * Returns the configured upgrade file.
	 *
	 * @return the configured upgrade file.
	 */
	public String getUpgradefile()
	{
		return this.upgradefile;
	}

	/**
	 * Sets the upgrade file to configure.
	 *
	 * @param upgradefile The upgrade file to configure.
	 */
	public void setUpgradefile( String upgradefile )
	{
		this.upgradefile = upgradefile;
	}

	/**
	 * Returns the configured target.
	 *
	 * @return The configured target.
	 */
	public String getTarget()
	{
		return this.upgradeTarget;
	}

	/**
	 * Sets the target to configure.
	 *
	 * @param target The target to configure.
	 */
	public void setTarget( String target )
	{
		this.upgradeTarget = target;
	}

	/**
	 * Returns if downgrades are allowed or not.
	 *
	 * @return True if downgrades are allowed, false otherwise.
	 */
	public boolean isDowngradeallowed()
	{
		return this.downgradeallowed;
	}

	/**
	 * Sets if downgrades are allowed or not.
	 *
	 * @param downgradeallowed Are downgrades allowed?
	 */
	public void setDowngradeallowed( boolean downgradeallowed )
	{
		this.downgradeallowed = downgradeallowed;
	}

	/**
	 * Validates the configuration of the Ant Task.
	 */
	@Override
	protected void validate()
	{
		super.validate();

		if( this.upgradefile == null )
			throw new BuildException( "The 'upgradefile' attribute is mandatory for the " + getTaskName() + " task" );
	}


	@Override
	public void execute()
	{
		validate();

//		The code below is meant to get to the REAL System.out.
//		PrintStream out = null;
//		try
//		{
//			Class main = Class.forName( "org.apache.tools.ant.Main" );
//			Field outField = main.getDeclaredField( "out" );
//			outField.setAccessible( true );
//			Object object = outField.get( null );
//			out = (PrintStream)object;
//		}
//		catch( SecurityException e )
//		{
//			throw new SystemException( e );
//		}
//		catch( NoSuchFieldException e )
//		{
//			throw new SystemException( e );
//		}
//		catch( IllegalArgumentException e )
//		{
//			throw new SystemException( e );
//		}
//		catch( IllegalAccessException e )
//		{
//			throw new SystemException( e );
//		}
//		catch( ClassNotFoundException e )
//		{
//			throw new SystemException( e );
//		}
//
//		out.println( "Dit is een test" );

		Project project = getProject();

		Runner runner = new Runner();
		runner.setProgressListener( new Progress( project, this ) );
		runner.setConnectionAttributes( "default", this.driver, this.url, this.username, this.password );
		for( Connection connection : this.connections )
			runner.setConnectionAttributes( connection.getName(), connection.getDriver(), connection.getUrl(),
					connection.getUsername(), connection.getPassword() );

		runner.setUpgradeFile( ResourceFactory.getResource( project.getBaseDir(), this.upgradefile ) );
		runner.setUpgradeTarget( this.upgradeTarget );
		runner.setDowngradeAllowed( this.downgradeallowed );

		try
		{
			runner.upgrade();
		}
		catch( FatalException e )
		{
			// TODO When debugging, we should give the whole exception, not only the message
			throw new BuildException( e.getMessage() );
		}
	}
}
