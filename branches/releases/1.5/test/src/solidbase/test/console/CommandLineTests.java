/*--
 * Copyright 2006 Ren� M. de Bloois
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
import solidbase.core.SystemException;
import solidbase.test.TestUtil;


public class CommandLineTests
{
	@Test
	public void testCommandLine() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:test2",
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-upgradefile", "testpatch1.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening file 'testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Upgrading to \"1.0.1\"\n" +
				"Creating table DBVERSION.\n" +
				"Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\".\n" +
				"Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}

	@Test
	public void testCommandLineNotPossible() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		try
		{
			Main.main0( "-verbose",
					"-driver", "org.hsqldb.jdbcDriver",
					"-url", "jdbc:hsqldb:mem:test22",
					"-username", "sa",
					"-password", "",
					"-target", "100.0.*",
					"-upgradefile", "testpatch1.sql" );

			Assert.fail( "Expected a SystemException" );
		}
		catch( SystemException e )
		{
			Assert.assertEquals( e.getMessage(), "Target 100.0.* is not a possible target" );
		}

		String output = TestUtil.generalizeOutput( console.getOutput() );

		Assert.assertEquals( output,
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening file 'testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n"
		);
	}

	@Test
	public void testCommandLineNoArguments() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		Main.main0();

		String output = TestUtil.generalizeOutput( console.getOutput() );

//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"usage: solidbase [-config <filename>] [-downgradeallowed] [-driver <classname>]\n" +
				"       [-dumplog <filename>] [-help] [-password <password>] [-target <version>]\n" +
				"       [-upgradefile <filename>] [-url <url>] [-username <username>] [-verbose]\n" +
				" -config <filename>        specifies the properties file to use\n" +
				" -downgradeallowed         allow downgrades to reach the target\n" +
				" -driver <classname>       sets the jdbc driverclass\n" +
				" -dumplog <filename>       export historical patch results to an xml file\n" +
				" -help                     Brings up this page\n" +
				" -password <password>      sets the password of the default username\n" +
				" -target <version>         sets the target version\n" +
				" -upgradefile <filename>   specifies the file containing the database upgrades\n" +
				" -url <url>                sets the url for the database\n" +
				" -username <username>      sets the default username to patch with\n" +
				" -verbose                  be extra verbose\n"
		);
	}

	@Test
	public void testCommandLineHelp() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		Main.main0( "-help" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"usage: solidbase [-config <filename>] [-downgradeallowed] [-driver <classname>]\n" +
				"       [-dumplog <filename>] [-help] [-password <password>] [-target <version>]\n" +
				"       [-upgradefile <filename>] [-url <url>] [-username <username>] [-verbose]\n" +
				" -config <filename>        specifies the properties file to use\n" +
				" -downgradeallowed         allow downgrades to reach the target\n" +
				" -driver <classname>       sets the jdbc driverclass\n" +
				" -dumplog <filename>       export historical patch results to an xml file\n" +
				" -help                     Brings up this page\n" +
				" -password <password>      sets the password of the default username\n" +
				" -target <version>         sets the target version\n" +
				" -upgradefile <filename>   specifies the file containing the database upgrades\n" +
				" -url <url>                sets the url for the database\n" +
				" -username <username>      sets the default username to patch with\n" +
				" -verbose                  be extra verbose\n"
		);
	}
}
