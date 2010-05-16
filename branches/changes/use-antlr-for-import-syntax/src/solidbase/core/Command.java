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
 * <p>
 * Represents a command in the upgrade file. A command can be transient or persistent. Transient commands start with
 * <code>--*</code>. Example of a transient command is:
 * </p>
 * <blockquote>
 * <pre>
 * --* SET USER A_USER
 * </pre>
 * </blockquote>
 * <p>
 * Transient commands are executed as they are encountered even when a patch package fails and is re-processed.
 * </p>
 * <p>
 * Example of a persistent command:
 * </p>
 * <blockquote>
 * <pre>
 * DROP TABLE A_TABLE
 * GO
 * </pre>
 * </blockquote>
 * <p>
 * </p>
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
	 * Is the command transient or not?
	 */
	private boolean isTransient;

	/**
	 * The line number in the upgrade file where the command is found.
	 */
	private int lineNumber;

	/**
	 * Instantiates a command.
	 * 
	 * @param command The text of the command.
	 * @param isTransient Is the command transient or not?
	 * @param lineNumber The line number in the upgrade file where the command is found.
	 */
	protected Command( String command, boolean isTransient, int lineNumber )
	{
		Assert.isTrue( lineNumber > 0 );
		this.command = command;
		this.isTransient = isTransient;
		this.lineNumber = lineNumber;
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
	 * Returns the line number in the upgrade file where the command is found.
	 * 
	 * @return The line number in the upgrade file where the command is found.
	 */
	public int getLineNumber()
	{
		return this.lineNumber;
	}
}
