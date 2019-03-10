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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.sql.SQLException;

import org.testng.annotations.Test;

import solidstack.io.SourceException;

public class ImportCSV
{
	static private final String db = "jdbc:hsqldb:mem:testImport";

	@Test
	public void testImportCSV() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "folder/testpatch-import-csv1.sql", db );
		try
		{
			patcher.upgrade( "1.0.2" );
			TestUtil.verifyVersion( patcher, "1.0.2", null, 23, null );
			TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP", 10 );
			TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP2", 6 );
			TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP7", 3 );

			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'x'", "2\n" );
			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP4 FROM TEMP3 WHERE TEMP1 = 2", "-)-\", \nTEST 'X" );
			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP2 FROM TEMP WHERE TEMP1 = 'y'", "2 2" );
			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP3 FROM TEMP WHERE TEMP1 = 'y'", " 3 " );
			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP3 FROM TEMP2 WHERE LINENUMBER = 101", "René" );
			TestUtil.assertQueryResultEquals( patcher, "SELECT DESC FROM TEMP7 WHERE ID = 2", "The second record" );

			patcher.upgrade( "1.0.3" );
			TestUtil.assertRecordCount( patcher.getCurrentDatabase(), "TEMP5", 3 );
			TestUtil.assertQueryResultEquals( patcher, "SELECT TEMP1 FROM TEMP4", null );
		}
		finally
		{
			patcher.end();
		}
	}

	@Test(dependsOnMethods="testImportCSV")
	public void testImportCSVNotExist() throws SQLException
	{
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "folder/testpatch-import-csv1.sql", db );

		try
		{
			patcher.upgrade( "1.0.4" );
			failBecauseExceptionWasNotThrown( FatalException.class );
		}
		catch( FatalException e )
		{
			assertThat( e.getMessage().replace( '\\', '/' ) ).contains( "java.io.FileNotFoundException: " ).contains( "folder/notexist.csv" );
		}

		patcher.end();
	}

	@Test
	public void testImportCSVLineNumber() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "testpatch-import-csv2.sql" );

		try
		{
			patcher.upgrade( "3" );
			failBecauseExceptionWasNotThrown( SourceException.class );
		}
		catch( SourceException e )
		{
			assertThat( e.getMessage() ).contains( "<separator>, <newline>" ).contains( "at line 52" );
		}

		try
		{
			patcher.upgrade( "5" );
			failBecauseExceptionWasNotThrown( ProcessException.class );
		}
		catch( ProcessException e )
		{
			assertThat( e.getMessage() ).contains( "integrity constraint violation" ).contains( "line 61" );
		}

		patcher.end();
	}

	@Test
	static public void testImportCSVProgress() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "testpatch-import-csv3.sql" );
		patcher.upgrade( "1" );
		patcher.end();
	}
}
