/*--
 * Copyright 2011 René M. de Bloois
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
 * SQL execution context.
 *
 * @author René M. de Bloois
 * @since Aug 2011
 */
public class SQLContext extends CommandContext
{
	private SQLSource source;

	/**
	 * Constructor.
	 *
	 * @param source The source for the SQL commands.
	 */
	public SQLContext( SQLSource source )
	{
		this.source = source;
	}

	/**
	 * Constructor.
	 *
	 * @param parent The parent execution context.
	 * @param source The source for the SQL commands.
	 */
	public SQLContext( CommandContext parent, SQLSource source )
	{
		super( parent );

		this.source = source;
	}

	/**
	 * Returns the source for the SQL commands.
	 *
	 * @return The source for the SQL commands.
	 */
	public SQLSource getSource()
	{
		return this.source;
	}
}
