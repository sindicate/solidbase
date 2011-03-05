/*--
 * Copyright 2010 Ren� M. de Bloois
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
import solidbase.util.BufferedReaderLineReader;
import solidbase.util.LineReader;


/**
 * The source for command from a patch file.
 * 
 * @author Ren� M. de Bloois
 */
public class PatchSource extends SQLSource
{
	/**
	 * Creates a new instance of a patch source.
	 * 
	 * @param in The reader which is used to read the SQL.
	 */
	protected PatchSource( LineReader in )
	{
		super( in );
	}

	/**
	 * Reads a statement from the patch file.
	 * 
	 * @return A statement from the patch file or null when no more statements are available.
	 */
	@Override
	public Command readCommand()
	{
		Command command;

		do
		{
			command = super.readCommand();
			Assert.notNull( command, "Premature end of file found" );

			if( command.isTransient() )
			{
				if( PatchFile.PATCH_END_PATTERN.matcher( command.getCommand() ).matches() )
					return null;
				if( PatchFile.PATCH_START_PATTERN.matcher( command.getCommand() ).matches() )
					command = null;
			}
		}
		while( command == null );

		return command;
	}
}
