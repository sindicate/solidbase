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

import solidstack.io.FileLocation;

/**
 * Represents a problem in the file (upgrade file or SQL file). A stack trace is not required. The failure and
 * line number are enough to identify corrective actions.
 *
 * @author René M. de Bloois
 */
// TODO Look for another name for this exception, like LocalizedFatalException and include the file's url
public class CommandFileException extends FatalException
{
	private static final long serialVersionUID = 1L;

	/**
	 * The file location where the problem is located.
	 */
	private FileLocation location;

	/**
	 * Constructor.
	 *
	 * @param message The failure message.
	 * @param location The file location where the problem is located.
	 */
	public CommandFileException( String message, FileLocation location )
	{
		super( message );
		this.location = location;
	}

	@Override
	public String getMessage()
	{
		return super.getMessage() + ", at " + this.location;
	}

	/**
	 * Returns the file location where the problem is located.
	 *
	 * @return The file location where the problem is located.
	 */
	public FileLocation getLocation()
	{
		return this.location;
	}
}
