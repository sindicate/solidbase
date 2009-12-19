/*--
 * Copyright 2006 René M. de Bloois
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
 * Represents a single patch in the patch file.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:15 PM
 */
public class Patch
{
	protected String source;
	protected String target;
	protected boolean switsj;
	protected boolean open;
	protected boolean init;
	protected boolean downgrade;
	protected int pos;

	protected Patch( String source, String target, boolean switsj, boolean open, boolean init, boolean downgrade )
	{
		this.source = source;
		this.target = target;
		this.switsj = switsj;
		this.downgrade = downgrade;
		this.open = open;
		this.init = init;
		this.pos = -1;
	}

	/**
	 * Is this patch a branch.
	 * 
	 * @return
	 */
	protected boolean isSwitch()
	{
		return this.switsj;
	}

	/**
	 * Gets the source version.
	 * 
	 * @return
	 */
	protected String getSource()
	{
		return this.source;
	}

	/**
	 * Gets the target version.
	 * 
	 * @return
	 */
	protected String getTarget()
	{
		return this.target;
	}

	/**
	 * Sets the byte position in the file for this patch.
	 * 
	 * @param pos
	 */
	protected void setPos( int pos )
	{
		this.pos = pos;
	}

	/**
	 * Gets the byte position in the file for this patch.
	 * 
	 * @return
	 */
	protected int getPos()
	{
		return this.pos;
	}

	/**
	 * Is this patch open.
	 * 
	 * @return
	 */
	protected boolean isOpen()
	{
		return this.open;
	}

	/**
	 * Is this the init patch.
	 * 
	 * @return
	 */
	protected boolean isInit()
	{
		return this.init;
	}

	/**
	 * Is this a downgrade.
	 * 
	 * @return
	 */
	protected boolean isDowngrade()
	{
		return this.downgrade;
	}

	protected boolean isUpgrade()
	{
		return !this.downgrade && !this.switsj;
	}

	@Override
	public String toString()
	{
		return "patch(source:" + this.source + ", target:" + this.target + ", open:" + this.open + ")";
	}
}
