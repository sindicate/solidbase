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

import solidbase.io.LineReader;
import solidbase.util.Assert;


/**
 * The source for commands from an upgrade file.
 *
 * @author René M. de Bloois
 */
public class UpgradeSource extends SQLSource
{
	/**
	 * Creates a new instance of an upgrade source.
	 *
	 * @param in The reader which is used to read the SQL.
	 */
	protected UpgradeSource( LineReader in )
	{
		super( in );
	}

	/**
	 * Reads a statement from the upgrade source.
	 *
	 * @return A statement from the upgrade source or null when no more statements are available.
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
				if( UpgradeFile.SEGMENT_END_PATTERN.matcher( command.getCommand() ).matches() )
					return null;
				if( UpgradeFile.SEGMENT_START_PATTERN.matcher( command.getCommand() ).matches() )
					command = null;
			}
		}
		while( command == null );

		return command;
	}
}
