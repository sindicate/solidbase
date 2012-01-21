/*--
 * Copyright 2006 Ren� M. de Bloois
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

import solidbase.io.FileLocation;
import solidbase.util.Assert;

/**
 * Represents a command in an upgrade or SQL file.
 *
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class Command
{
	/**
	 * The text of the command.
	 */
	private String command;

	/**
	 * Is the command transient or not?
	 */
	private boolean isTransient;

	/**
	 * The file location where the command is encountered.
	 */
	private FileLocation location;

	/**
	 * Constructor.
	 *
	 * @param command The text of the command.
	 * @param isTransient Is the command transient or not?
	 * @param lineNumber The line number in the upgrade file where the command is encountered.
	 */
	public Command( String command, boolean isTransient, FileLocation location )
	{
		Assert.notNull( command );

		this.command = command;
		this.isTransient = isTransient;
		this.location = location;
	}

	/**
	 * Indicates if the command is transient or not.
	 *
	 * @return true if the command is transient, false otherwise.
	 */
	public boolean isTransient()
	{
		return this.isTransient;
	}

	/**
	 * Indicates if the command is persistent or not.
	 *
	 * @return true if the command is persistent, false otherwise.
	 */
	public boolean isPersistent()
	{
		return !this.isTransient;
	}

	/**
	 * Returns the text of the command.
	 *
	 * @return the text of the command.
	 */
	public String getCommand()
	{
		return this.command;
	}

	/**
	 * Sets the command text.
	 *
	 * @param command the command text.
	 */
	public void setCommand( String command )
	{
		this.command = command;
	}

	/**
	 * Returns the file location where the command is encountered.
	 *
	 * @return The file location where the command is encountered.
	 */
	public FileLocation getLocation()
	{
		return this.location;
	}

	@Override
	public String toString()
	{
		return this.command;
	}
}
