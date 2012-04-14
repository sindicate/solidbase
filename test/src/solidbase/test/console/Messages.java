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

package solidbase.test.console;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.Main;
import solidbase.core.TestUtil;
import solidbase.test.mocks.MockConsole;

public class Messages
{
	@Test
	static public void testMessageBeforeListener() throws Exception
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.pass2( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:testdb",
				"-username", "sa",
				"-password", "",
				"-target", "1.0.2",
				"-upgradefile", "folder/testpatch-import1.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-import1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating control tables..\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\".\n" +
				"Starting import.....\n" + // Message before listener execution
				"    Generating SQLException.\n" + // There should be a dot here
				"    Importing with linenumber..\n" +
				"112\n" +
				"113\n" +
				"114.\n" +
				"        Importing with column list\n" +
				"            And deeper..\n" +
				"23Y\n" +
				"47Y.\n" +
				"        Importing from external file.\n" +
				"        Importing through update.\n" +
		        "2360\n" +
		        "4770.\n" +
		        "        Importing through delete.\n" +
		        "2360.\n" +
		        "DEBUG: version=1.0.1, target=1.0.2, statements=19\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrade complete.\n"
		);
	}
}
