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

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.Main;
import solidbase.Progress;
import solidbase.config.Configuration;
import solidbase.config.Manipulator;
import solidbase.config.Options;
import solidbase.core.TestUtil;


public class ThroughConsoleTests
{
	@Test
	public void testConsole() throws Exception
	{
		// Mock the name of the property file

		Mockit.tearDownMocks();
		Mockit.redefineMethods( Configuration.class, new MockConfiguration( "solidbase1.properties" ) );

		// Test the mock itself
		Configuration configuration = new Configuration( new Progress( null, false ), 2, new Options( false, false, null, null, null, null, null, null, null, null, false, false ) );
		Assert.assertEquals( Manipulator.getConfigurationPropertiesFile( configuration ).getName(), "solidbase1.properties" );

		// Start test

		MockConsole console = new MockConsole();
		console.addAnswer( "" );

		Main.console = console;

		Main.pass2( "-verbose" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:\\...\\solidbase1.properties\n" +
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Opening file 'testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
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
	public void testConsole2() throws Exception
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "" );

		Main.console = console;

		Main.pass2( "-verbose", "-config", "solidbase2.properties" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:\\...\\solidbase2.properties\n" +
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Opening file 'testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
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

	@Test(dependsOnMethods="testConsole2")
	public void testPrint1() throws Exception
	{
		MockConsole console = new MockConsole();
		Main.console = console;

		Main.pass2( "-verbose", "-config", "solidbase2.properties", "-upgradefile", "testpatch-print1.sql", "-password", "" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file X:\\...\\solidbase2.properties\n" +
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Opening file 'testpatch-print1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"DEBUG: version=1.0.2, target=null, statements=2\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrading \"1.0.2\" to \"1.0.3\"\n" +
				"1.\n" + // Concat not working with HSQLDB 2.0.0
				"DEBUG: version=1.0.2, target=1.0.3, statements=1\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.3\".\n"
		);
	}
}
