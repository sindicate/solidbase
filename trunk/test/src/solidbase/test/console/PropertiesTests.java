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

package solidbase.test.console;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.Main;
import solidbase.core.TestUtil;
import solidbase.test.mocks.MockConsole;


public class PropertiesTests
{
	@Test
	static public void testConsole() throws Exception
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "" );
		Main.console = console;

		Main.pass2( "-verbose", "-config", "solidbase1.properties" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:/.../solidbase1.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': The database is unmanaged.\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating table DBVERSION.\n" +
				"    Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\".\n" +
				"    Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrade complete.\n"
		);
	}

	@Test
	static public void testConsole2() throws Exception
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "" );
		Main.console = console;

		Main.pass2( "-verbose", "-config", "solidbase2.properties" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:/.../solidbase2.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': The database is unmanaged.\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating table DBVERSION.\n" +
				"    Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\".\n" +
				"    Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrade complete.\n"
		);
	}

	@Test(dependsOnMethods="testConsole2")
	static public void testPrint1() throws Exception
	{
		MockConsole console = new MockConsole();
		Main.console = console;

		Main.pass2( "-verbose", "-config", "solidbase2.properties", "-upgradefile", "testpatch-print1.sql", "-password", "" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:/.../solidbase2.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-print1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"DEBUG: version=1.0.2, target=null, statements=2\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrading \"1.0.2\" to \"1.0.3\"\n" +
				"1.\n" + // Concat not working with HSQLDB 2.0.0
				"DEBUG: version=1.0.2, target=1.0.3, statements=1\n" +
				"\n" +
				"Current database version is \"1.0.3\".\n" +
				"Upgrade complete.\n"
		);
	}

	@Test
	static public void testSqlParameters() throws Exception
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "" );
		Main.console = console;

		Main.pass2( "-verbose", "-config", "testsql-parameters2.properties" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:/.../testsql-parameters2.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': ..\n" +
				"val1..\n" +
				"Execution complete.\n" +
				"\n"
		);
	}

	@Test
	static public void testUpgradeParameters() throws Exception
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "" );
		Main.console = console;

		Main.pass2( "-verbose", "-config", "testpatch-parameters2.properties" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:/.../testpatch-parameters2.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': The database is unmanaged.\n" +
				"Setting up control tables to \"1.1\"\n" +
				"Opening file 'X:/.../setup-1.1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"....\n" +
				"Upgrading to \"1\"..\n" +
				"val1.\n" +
				"DEBUG: version=null, target=1, statements=3\n" +
				"\n" +
				"Current database version is \"1\".\n" +
				"Upgrade complete.\n"
		);
	}
}
