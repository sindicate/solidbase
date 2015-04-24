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
	protected boolean branch;
	protected boolean returnBranch;
	protected boolean open;
	protected boolean init;
	protected int pos;

	protected Patch( String source, String target, boolean branch, boolean returnBranch, boolean open, boolean init )
	{
		Assert.isTrue( !( branch && returnBranch ) );

		this.source = source;
		this.target = target;
		this.branch = branch;
		this.returnBranch = returnBranch;
		this.open = open;
		this.init = init;
		this.pos = -1;
	}

	/**
	 * Is this patch a branch.
	 * 
	 * @return
	 */
	protected boolean isBranch()
	{
		return this.branch;
	}

	/**
	 * Is this patch a return from a branch.
	 * 
	 * @return
	 */
	protected boolean isReturnBranch()
	{
		return this.returnBranch;
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
}
