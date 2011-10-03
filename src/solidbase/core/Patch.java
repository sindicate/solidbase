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
	/**
	 * The possible types of a patch.
	 */
	public enum Type
	{
		/**
		 * An init patch is used to create or maintain the DBVERSION and DBVERSIONLOG tables.
		 */
		SETUP,
		/**
		 * An upgrade patch is used to upgrade the database.
		 */
		UPGRADE,
		/**
		 * A switch patch is used to switch between upgrade paths (branches).
		 */
		SWITCH,
		/**
		 * A downgrade patch is used during development to downgrade the database. For example, this makes it possible
		 * to switch back to a previous stable branch.
		 */
		DOWNGRADE
	}

	/**
	 * The type of this patch.
	 */
	protected Type type;

	/**
	 * The source version of this patch.
	 */
	protected String source;

	/**
	 * The target version of this patch.
	 */
	protected String target;

	/**
	 * Is this patch open (unfinished).
	 */
	protected boolean open;

	/**
	 * The line number of this patch.
	 */
	protected int lineNumber;

	/**
	 * Constructs a new patch.
	 * 
	 * @param type The type of the patch.
	 * @param source The source version of this patch.
	 * @param target The target version of this patch.
	 * @param open Is this patch open (unfinished).
	 */
	protected Patch( Type type, String source, String target, boolean open )
	{
		this.type = type;
		this.source = source;
		this.target = target;
		this.open = open;
		this.lineNumber = -1;
	}

	/**
	 * Is this patch a switch.
	 * 
	 * @return True if this patch is a switch, false otherwise.
	 */
	protected boolean isSwitch()
	{
		return this.type == Type.SWITCH;
	}

	/**
	 * Gets the source version.
	 * 
	 * @return The source version.
	 */
	public String getSource()
	{
		return this.source;
	}

	/**
	 * Gets the target version.
	 * 
	 * @return The target version.
	 */
	public String getTarget()
	{
		return this.target;
	}

	/**
	 * Sets the line number in the file for this patch.
	 * 
	 * @param lineNumber The line number.
	 */
	protected void setLineNumber( int lineNumber )
	{
		this.lineNumber = lineNumber;
	}

	/**
	 * Gets the line number in the file for this patch.
	 * 
	 * @return The line number in the file for this patch.
	 */
	protected int getLineNumber()
	{
		return this.lineNumber;
	}

	/**
	 * Is this patch open.
	 * 
	 * @return True if this patch is open (unfinished), false otherwise.
	 */
	protected boolean isOpen()
	{
		return this.open;
	}

	/**
	 * Is this an init patch.
	 * 
	 * @return True if this patch is an init patch, false otherwise.
	 */
	protected boolean isSetup()
	{
		return this.type == Type.SETUP;
	}

	/**
	 * Is this a downgrade patch.
	 * 
	 * @return True if this patch is a downgrade patch, false otherwise.
	 */
	protected boolean isDowngrade()
	{
		return this.type == Type.DOWNGRADE;
	}

	/**
	 * Is this patch is normal upgrade patch?
	 * 
	 * @return True if this patch is a normal upgrade patch, false otherwise.
	 */
	protected boolean isUpgrade()
	{
		return this.type == Type.UPGRADE;
	}

	@Override
	public String toString()
	{
		return "patch(source:" + this.source + ", target:" + this.target + ", open:" + this.open + ")";
	}

	/**
	 * Returns the type of this patch.
	 * 
	 * @return The type of this patch.
	 */
	public Type getType()
	{
		return this.type;
	}
}
