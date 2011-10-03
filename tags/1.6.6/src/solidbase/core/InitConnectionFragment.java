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

package solidbase.core;

/**
 * An SQL fragment.
 * 
 * @author René de Bloois
 */
public class InitConnectionFragment extends Fragment
{
	/**
	 * The name of the connection. If null then applies to all connections.
	 */
	protected String connectionName;

	/**
	 * The name of the user. If null then applies to all users.
	 */
	protected String userName;

	/**
	 * Constructor.
	 * 
	 * @param lineNumber The line number of the fragment in the original file.
	 * @param text The text of the fragment.
	 * @param connectionName The name of the connection.
	 * @param userName The name of the user.
	 */
	protected InitConnectionFragment( int lineNumber, String text, String connectionName, String userName )
	{
		super( lineNumber, text );
		this.connectionName = connectionName;
		this.userName = userName;
	}

	/**
	 * Returns the name of the connection to initialize. If null this applies to all connections.
	 *
	 * @return The name of the connection to initialize.
	 */
	public String getConnectionName()
	{
		return this.connectionName;
	}

	/**
	 * Returns the name of the user to initialize. If null this applies to all connections.
	 *
	 * @return The name of the user to initialize.
	 */
	public String getUserName()
	{
		return this.userName;
	}
}
