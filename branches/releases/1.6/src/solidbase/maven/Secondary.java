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


/**
 * A secondary connection used during configuration of the Maven Plugin.
 * 
 * @author René M. de Bloois
 */
public class Secondary
{
	private String name;
	private String driver;
	private String url;
	private String username;
	private String password;

	/**
	 * Constructor.
	 */
	public Secondary()
	{
		super();
	}

	/**
	 * Returns the configured name of the secondary connection.
	 * 
	 * @return The configured name of the secondary connection.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the configured database driver of the secondary connection.
	 * 
	 * @return The configured database driver of the secondary connection.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Returns the configured database url of the secondary connection.
	 * 
	 * @return The configured database url of the secondary connection.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Returns the configured user name of the secondary connection.
	 * 
	 * @return The configured user name of the secondary connection.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Returns the configured password of the secondary connection.
	 * 
	 * @return The configured password of the secondary connection.
	 */
	public String getPassword()
	{
		return this.password;
	}
}
