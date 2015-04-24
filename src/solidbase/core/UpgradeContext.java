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
 * Upgrade execution context.
 *
 * @author René M. de Bloois
 * @since Aug 2011
 */
public class UpgradeContext extends CommandContext
{
	private SQLSource source;

	/**
	 * Indicates that the statements are transient and should not be counted.
	 */
	private boolean dontCount;

	/**
	 * Constructor.
	 *
	 * @param source The source for the upgrade commands.
	 */
	public UpgradeContext( SQLSource source )
	{
		this.source = source;
	}

	/**
	 * Constructor.
	 *
	 * @param parent The parent upgrade context.
	 * @param source The source for the upgrade commands.
	 */
	public UpgradeContext( UpgradeContext parent, SQLSource source )
	{
		super( parent );
		this.dontCount = parent.dontCount;

		this.source = source;
	}

	/**
	 * Are we in transient mode?
	 *
	 * @return True if in transient mode, false otherwise.
	 */
	public boolean isDontCount()
	{
		return this.dontCount;
	}

	/**
	 * Enable or disable transient mode.
	 *
	 * @param dontCount True for transient, false for pesistent mode.
	 */
	public void setDontCount( boolean dontCount )
	{
		this.dontCount = dontCount;
	}

	/**
	 * Returns the source for the SQL commands.
	 *
	 * @return the source for the SQL commands.
	 */
	public SQLSource getSource()
	{
		return this.source;
	}
}
