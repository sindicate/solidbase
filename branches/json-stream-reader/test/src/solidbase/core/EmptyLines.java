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

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.util.FileResource;
import solidbase.util.URLRandomAccessLineReader;


public class EmptyLines
{
	@Test
	public void testEmptyLines() throws IOException
	{
		URLRandomAccessLineReader ralr = new URLRandomAccessLineReader( new FileResource( "testpatch-emptylines.sql" ) );
		UpgradeFile upgradeFile = new UpgradeFile( ralr );
		upgradeFile.scan();
		UpgradeSegment segment = upgradeFile.getSegment( "1.0.1", "1.0.2" );
		assert segment != null;
		UpgradeSource source = upgradeFile.gotoSegment( segment );
		Command command = source.readCommand();
		assert command != null;
		Assert.assertEquals( command.getCommand(), "INSERT 'This insert contains\n" +
				"\n" +
				"a couple\n" +
				"\n" +
				"\n" +
				"of empty lines'\n" +
		"\n" );
		upgradeFile.close();
	}
}
