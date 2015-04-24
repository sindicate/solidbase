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
import solidbase.core.FatalException;
import solidbase.core.TestUtil;
import solidbase.test.mocks.MockConsole;


public class CommandLineTests
{
	static private final String db = "jdbc:hsqldb:mem:testCommandLine";
	static private final String db2 = "jdbc:hsqldb:mem:testCommandLine2";

	@Test
	static public void testCommandLine() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", db,
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-upgradefile", "testpatch1.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
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

	@Test(dependsOnMethods="testCommandLine")
	static public void testDumpLog() throws Exception
	{
		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-driver", "org.hsqldb.jdbcDriver",
				"-url", db,
				"-username", "sa",
				"-password", "",
				"-upgradefile", "testpatch1.sql",
				"-dumplog", "-" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
		// TODO Also test dump to file
		Assert.assertEquals( output, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
		"    Encoding is 'ISO-8859-1'\n" );
	}

	@Test
	static public void testCommandLineNoTarget() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", db2,
				"-username", "sa",
				"-password", "",
				"-upgradefile", "testpatch1.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
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
	static public void testCommandLineNotPossible() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		try
		{
			Main.main0( "-driver", "org.hsqldb.jdbcDriver",
					"-url", db2,
					"-username", "sa",
					"-password", "",
					"-target", "100.0.*",
					"-upgradefile", "testpatch1.sql" );

			Assert.fail( "Expected a FatalException" );
		}
		catch( FatalException e )
		{
			Assert.assertTrue( e.getMessage().equals( "Target 100.0.* is not reachable from version <no version>" ) );
		}

		String output = TestUtil.generalizeOutput( console.getOutput() );
		Assert.assertEquals( output,
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Upgrade aborted.\n"
		);
	}

	@Test
	static public void testCommandLineNoArguments() throws Exception
	{
		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0();

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"usage: solidbase [-config <filename>] [-D <property=value>] [-downgradeallowed]\n" +
				"       [-driver <classname>] [-dumplog <filename>] [-help] [-password\n" +
				"       <password>] [-sqlfile <filename>] [-target <version>] [-upgradefile\n" +
				"       <filename>] [-url <url>] [-username <username>] [-verbose]\n" +
				" -config <filename>        specifies a properties file to use\n" +
				" -D <property=value>       parameter to the SQL file or upgrade file\n" +
				" -downgradeallowed         allow downgrades to reach the target\n" +
				" -driver <classname>       sets the JDBC driverclass\n" +
				" -dumplog <filename>       export historical upgrade results to an XML file\n" +
				" -help                     Brings up this page\n" +
				" -password <password>      sets the password of the default user\n" +
				" -sqlfile <filename>       specifies an SQL file to execute\n" +
				" -target <version>         sets the target version to upgrade to\n" +
				" -upgradefile <filename>   specifies the file containing the database upgrades\n" +
				" -url <url>                sets the URL for the database\n" +
				" -username <username>      sets the default user name to connect with\n" +
				" -verbose                  be extra verbose\n"
		);
	}

	@Test
	static public void testCommandLineHelp() throws Exception
	{
		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-help" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"usage: solidbase [-config <filename>] [-D <property=value>] [-downgradeallowed]\n" +
				"       [-driver <classname>] [-dumplog <filename>] [-help] [-password\n" +
				"       <password>] [-sqlfile <filename>] [-target <version>] [-upgradefile\n" +
				"       <filename>] [-url <url>] [-username <username>] [-verbose]\n" +
				" -config <filename>        specifies a properties file to use\n" +
				" -D <property=value>       parameter to the SQL file or upgrade file\n" +
				" -downgradeallowed         allow downgrades to reach the target\n" +
				" -driver <classname>       sets the JDBC driverclass\n" +
				" -dumplog <filename>       export historical upgrade results to an XML file\n" +
				" -help                     Brings up this page\n" +
				" -password <password>      sets the password of the default user\n" +
				" -sqlfile <filename>       specifies an SQL file to execute\n" +
				" -target <version>         sets the target version to upgrade to\n" +
				" -upgradefile <filename>   specifies the file containing the database upgrades\n" +
				" -url <url>                sets the URL for the database\n" +
				" -username <username>      sets the default user name to connect with\n" +
				" -verbose                  be extra verbose\n"
		);
	}

	@Test
	static public void testCommandLineSQLFile() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-driver", "org.hsqldb.jdbcDriver",
				"-url", db2,
				"-username", "sa",
				"-password", "",
				"-sqlfile", "testsql-sections.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql-sections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Creating table USERS.\n" +
				"Filling USERS\n" +
				"    Inserting admin user.\n" +
				"    Inserting 3 users...\n" +
				"    Inserting 3 users.\n" +
				"Adding more USERS\n" +
				"    Inserting 3 users.\n" +
				"    Inserting 3 users....\n" +
				"Execution complete.\n\n"
		);
	}

	@Test
	static public void testSkip() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", db2,
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-upgradefile", "testpatch-skip.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-skip.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Setting up control tables to \"1.1\"...\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Section 1.\n" +
				"    Section 3.\n" +
				"    Section 5.\n" +
				"DEBUG: version=null, target=1.0.1, statements=4\n" +
				"\n" +
				"Current database version is \"1.0.1\".\n" +
				"Upgrade complete.\n"
		);
	}

	@Test
	static public void testSqlParameters() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", db2,
				"-username", "sa",
				"-password", "",
				"-sqlfile", "testsql-parameter2.sql",
				"-Dpar1=val1",
				"-Dpar2=" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"..\n" +
				"val1..\n" +
				"Execution complete.\n" +
				"\n"
		);
	}

	@Test
	static public void testUpgradeParameters() throws Exception
	{
		TestUtil.dropHSQLDBSchema( db2, "sa", null );

		MockConsole console = new MockConsole();
		Main.console = console;

		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", db2,
				"-username", "sa",
				"-password", "",
				"-upgradefile", "testpatch-parameter2.sql",
				"-Dpar1=val1",
				"-Dpar2=" );

		String output = TestUtil.generalizeOutput( console.getOutput() );
//		System.out.println( "[[[" + output + "]]]" );
		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
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
