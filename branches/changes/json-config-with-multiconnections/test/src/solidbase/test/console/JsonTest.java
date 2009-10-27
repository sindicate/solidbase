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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.Test;


public class JsonTest
{
	@Test(groups={"unit"})
	public void testJson() throws IOException, ParseException
	{
		InputStream is = JsonTest.class.getClassLoader().getResourceAsStream( "test1.json" );
		assert is != null;
		Reader reader = new InputStreamReader( is, "UTF-8" );
		new JSONParser().parse( reader );
	}

	/*
	@Test(groups={"integration"})
	public void testCommandLineJson() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		Main.main0( "-verbose", "-jsonconfig", "test1.json" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.0\\.x\\s+\\(C\\) 2006-200\\d René M\\. de Bloois", "SolidBase v1.0.x (C) 2006-200x René M. de Bloois" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"SolidBase v1.0.x (C) 2006-200x René M. de Bloois\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'file:/.../testpatch1.sql'\n" +
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
	 */
}
