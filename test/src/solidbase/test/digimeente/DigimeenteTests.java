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

package solidbase.test.digimeente;

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import solidbase.Main;
import solidbase.Progress;
import solidbase.config.Configuration;
import solidbase.config.Manipulator;
import solidbase.core.Patcher;
import solidbase.test.console.MockConsole;


public class DigimeenteTests
{
	@BeforeMethod
	protected void init()
	{
		Patcher.end();
	}

	@Test
	public void testDigimeenteCommandLineWithClass() throws Exception
	{
		Mockit.tearDownMocks();
		Mockit.redefineMethods( Configuration.class, new MockConfiguration( "solidbase-digimeente.properties" ) );

		// Test the mock itself
		Configuration configuration = new Configuration( new Progress( null, false ), 2, null, null, null, null, null, null );
		Assert.assertEquals( Manipulator.getConfigurationPropertiesFile( configuration ).getName(), "solidbase-digimeente.properties" );

		MockConsole console = new MockConsole();
		console.addAnswer( "Zaanstad-slot1" );
		console.addAnswer( "" );
		console.addAnswer( "1.0.2" );

		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main0( "-verbose" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.0\\.x\\s+\\(C\\) 2006-200\\d René M\\. de Bloois", "SolidBase v1.0.x (C) 2006-200x René M. de Bloois" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//		output = output.replaceAll( "\\\t", "\\t" );

		//		new FileOutputStream( new File( "dump.txt" ) ).write( output.getBytes() );
		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file C:\\...\\solidbase-digimeente.properties\n" +
				"SolidBase v1.0.x (C) 2006-200x René M. de Bloois\n" +
				"\n" +
				"Available database:\n" +
				"    Zaanstad-slot1\n" +
				"    Zaanstad-slot2\n" +
				"Select a database from the above: \n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:digimeente1, user=sa\n" +
				"Connecting to database 'Zaanstad-slot1', application 'midoffice'...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
				"Opening patchfile 'file:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Possible targets are: 1.0.1, 1.0.2\n" +
				"Input target version: Patching \"null\" to \"1.0.1\"\n" +
				"Creating table DBVERSION.\n" +
				"Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Patching \"1.0.1\" to \"1.0.2\"DEBUG: version=1.0.1, target=null, statements=2\n" +
				"Creating table USERS.\n" +
				"Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"The database has been patched.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}

	@Test
	public void testDigimeenteCommandLineWithScript() throws Exception
	{
		Mockit.tearDownMocks();
		Mockit.redefineMethods( Configuration.class, new MockConfiguration( "solidbase-digimeente2.properties" ) );

		MockConsole console = new MockConsole();
		console.addAnswer( "Zaanstad-slot2" );
		console.addAnswer( "" );
		console.addAnswer( "1.0.2" );

		Main.console = console;

		Main.main0( "-verbose" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.0\\.x\\s+\\(C\\) 2006-200\\d René M\\. de Bloois", "SolidBase v1.0.x (C) 2006-200x René M. de Bloois" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//		output = output.replaceAll( "\\\t", "\\t" );

		//		new FileOutputStream( new File( "dump.txt" ) ).write( output.getBytes() );
		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"Reading property file C:\\...\\solidbase-digimeente2.properties\n" +
				"SolidBase v1.0.x (C) 2006-200x René M. de Bloois\n" +
				"\n" +
				"Available database:\n" +
				"    Zaanstad-slot1\n" +
				"    Zaanstad-slot2\n" +
				"Select a database from the above: \n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:digimeente2, user=sa\n" +
				"Connecting to database 'Zaanstad-slot2', application 'midoffice'...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
				"Opening patchfile 'file:/.../testpatch1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Possible targets are: 1.0.1, 1.0.2\n" +
				"Input target version: Patching \"null\" to \"1.0.1\"\n" +
				"Creating table DBVERSION.\n" +
				"Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Patching \"1.0.1\" to \"1.0.2\"DEBUG: version=1.0.1, target=null, statements=2\n" +
				"Creating table USERS.\n" +
				"Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"The database has been patched.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}
}
