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

package ronnie.dbpatcher.test.console;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ronnie.dbpatcher.Main;
import ronnie.dbpatcher.core.Patcher;
import ronnie.dbpatcher.core.SystemException;


public class CommandLineTests
{
	@BeforeClass
	protected void init()
	{
		Patcher.end();
	}

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
				"-patchfile", "dbpatch-hsqldb-example.sql" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "SolidBase v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"SolidBase v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Patching \"null\" to \"1.0.1\"\n" +
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
	public void testCommandLineNotPossible() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		try
		{
			Main.main0( "-verbose",
					"-driver", "org.hsqldb.jdbcDriver",
					"-url", "jdbc:hsqldb:mem:test2",
					"-username", "sa",
					"-password", "",
					"-target", "100.0.*",
					"-patchfile", "dbpatch-hsqldb-example.sql" );

			Assert.fail( "Expected a SystemException" );
		}
		catch( SystemException e )
		{
			Assert.assertEquals( e.getMessage(), "Target 100.0.* is not a possible target" );
		}

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "SolidBase v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//output = output.replaceAll( "\\n\\s+at\\s+.+", "" );

		Assert.assertEquals( output,
				"SolidBase v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n"
		);
	}
}
