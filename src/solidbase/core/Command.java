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

import solidbase.util.Assert;
import solidstack.io.SourceLocation;

/**
 * Represents a command in an upgrade or SQL file.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class Command
{
	/**
	 * The text of the command.
	 */
	private String command;

	/**
	 * Is the command an annotation?
	 */
	private boolean isAnnotation;

	/**
	 * The file location where the command is encountered.
	 */
	private SourceLocation location;

	/**
	 * Constructor.
	 *
	 * @param command The text of the command.
	 * @param isAnnotation Is the command an annotation?
	 * @param location The location where the command is encountered.
	 */
	public Command( String command, boolean isAnnotation, SourceLocation location )
	{
		Assert.notNull( command );

		this.command = command;
		this.isAnnotation = isAnnotation;
		this.location = location;
	}

	/**
	 * Indicates if the command is an annotation.
	 *
	 * @return true if the command is an annotation, false otherwise.
	 */
	public boolean isAnnotation()
	{
		return this.isAnnotation;
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
	public SourceLocation getLocation()
	{
		return this.location;
	}

	@Override
	public String toString()
	{
		return this.command;
	}
}
