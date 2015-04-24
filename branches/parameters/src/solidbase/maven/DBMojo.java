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

import solidbase.core.Runner;


/**
 * Maven plugin that has database connection functionality.
 *
 * @author René de Bloois
 */
abstract public class DBMojo extends AbstractMojo
{
	/**
	 * The Maven Project Object
	 */
	public MavenProject project;

	/**
	 * Database driver class.
	 */
	public String driver;

	/**
	 * Database URL.
	 */
	public String url;

	/**
	 * Database username.
	 */
	public String username;

	/**
	 * Database password.
	 */
	public String password;

	/**
	 * Skip execution of the plugin.
	 */
	public boolean skip;

	/**
	 * An array of secondary connections.
	 */
	public Secondary[] connections;

	/**
	 * An array of parameters.
	 */
	public Parameter[] parameters;

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

	/**
	 * Prepares the core's Runner.
	 *
	 * @return The Runner.
	 */
	public Runner prepareRunner()
	{
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

		if( this.parameters != null )
			for( Parameter parameter : this.parameters )
				runner.addParameter( parameter.getName(), parameter.getValue() );

		return runner;
	}
}
