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
	public enum Type { INIT, UPGRADE, SWITCH, DOWNGRADE };

	protected Type type;
	protected String source;
	protected String target;
	protected boolean open;
	protected int pos;

	protected Patch( Type type, String source, String target, boolean open )
	{
		this.type = type;
		this.source = source;
		this.target = target;
		this.open = open;
		this.pos = -1;
	}

	/**
	 * Is this patch a branch.
	 * 
	 * @return
	 */
	protected boolean isSwitch()
	{
		return this.type == Type.SWITCH;
	}

	/**
	 * Gets the source version.
	 * 
	 * @return
	 */
	public String getSource()
	{
		return this.source;
	}

	/**
	 * Gets the target version.
	 * 
	 * @return
	 */
	public String getTarget()
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
		return this.type == Type.INIT;
	}

	/**
	 * Is this a downgrade.
	 * 
	 * @return
	 */
	protected boolean isDowngrade()
	{
		return this.type == Type.DOWNGRADE;
	}

	protected boolean isUpgrade()
	{
		return this.type == Type.UPGRADE;
	}

	@Override
	public String toString()
	{
		return "patch(source:" + this.source + ", target:" + this.target + ", open:" + this.open + ")";
	}

	public Type getType()
	{
		return this.type;
	}
}
