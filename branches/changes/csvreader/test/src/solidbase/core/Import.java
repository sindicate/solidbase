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

import solidbase.core.PatchProcessor;

public class Import
{
	@Test(groups="new")
	public void testImport() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		PatchProcessor patcher = Setup.setupPatchProcessor( "testpatch-import1.sql" );

		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 13, null );
		TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP", 10 );

		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'x'", "2\n" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP4 FROM TEMP3 WHERE TEMP1 = 2", "-)-\", \nTEST 'X" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'y'", "2 2" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP3 FROM TEMP WHERE TEMP1 = 'y'", " 3 " );

		patcher.patch( "1.0.3" );
		TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP1 FROM TEMP4", null );

		patcher.end();
	}

	@Test(groups="new")
	public void testImportLineNumber() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		PatchProcessor patcher = Setup.setupPatchProcessor( "testpatch-import2.sql" );

		try
		{
			patcher.patch( "3" );
			assert false;
		}
		catch( CommandFileException e )
		{
			assert e.getMessage().contains( "<separator>, <newline>" );
			assert e.getMessage().contains( "at line 53" );
		}

		try
		{
			patcher.patch( "4" );
			assert false;
		}
		catch( CommandFileException e )
		{
			assert e.getMessage().contains( "Unexpected \"" );
			assert e.getMessage().contains( "at line 62" );
		}

		try
		{
			patcher.patch( "5" );
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
