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

import java.sql.SQLException;

import org.testng.annotations.Test;

public class Import
{
	static private final String db = "jdbc:hsqldb:mem:testImport";

	@Test
	public void testImport() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "folder/testpatch-import1.sql", db );

		patcher.upgrade( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 18, null );
		TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP", 10 );
		TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP2", 6 );

		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'x'", "2\n" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP4 FROM TEMP3 WHERE TEMP1 = 2", "-)-\", \nTEST 'X" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'y'", "2 2" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP3 FROM TEMP WHERE TEMP1 = 'y'", " 3 " );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP3 FROM TEMP2 WHERE LINENUMBER = 101", "René" );

		patcher.upgrade( "1.0.3" );
		TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP5", 3 );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP1 FROM TEMP4", null );

		patcher.end();
	}

	@Test(dependsOnMethods="testImport")
	public void testImportNotExist() throws SQLException
	{
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "folder/testpatch-import1.sql", db );

		try
		{
			patcher.upgrade( "1.0.4" );
			assert false : "Expected a FatalException";
		}
		catch( FatalException e )
		{
			String message = e.getMessage().replace( '\\', '/' );
			assert message.contains( "java.io.FileNotFoundException: " ) : message;
			assert message.contains( "folder/notexist.csv" ) : message;
		}

		patcher.end();
	}

	@Test(dependsOnMethods="testImportNotExist")
	public void testImportJSV() throws SQLException
	{
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "folder/testpatch-import1.sql", db );

		patcher.upgrade( "1.0.5" );

		patcher.end();
	}

	@Test
	public void testImportLineNumber() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "testpatch-import2.sql" );

		try
		{
			patcher.upgrade( "3" );
			assert false;
		}
		catch( CommandFileException e )
		{
			assert e.getMessage().contains( "<separator>, <newline>" );
			assert e.getMessage().contains( "at line 53" ) : "Wrong error message: " + e.getMessage();
		}

		try
		{
			patcher.upgrade( "4" );
			assert false;
		}
		catch( CommandFileException e )
		{
			assert e.getMessage().contains( "Unexpected \"" );
			assert e.getMessage().contains( "at line 62" );
		}

		try
		{
			patcher.upgrade( "5" );
			assert false;
		}
		catch( SQLExecutionException e )
		{
			assert e.getMessage().contains( "integrity constraint violation" );
			assert e.getMessage().contains( "executing line 70" );
		}

		patcher.end();
	}
}
