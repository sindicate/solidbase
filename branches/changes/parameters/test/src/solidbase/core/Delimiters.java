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

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Delimiter.Type;

public class Delimiters
{
	@Test
	public void testDelimiterRegexpCharacter()
	{
		String contents = "COMMAND\n^\n";
		SQLSource source = new SQLSource( contents );
		source.setDelimiters( new Delimiter[] { new Delimiter( "^", Type.ISOLATED ) } );
		Command command = source.readCommand();
		assert command != null;
		Assert.assertEquals( command.getCommand(), "COMMAND\n" );
	}
}
