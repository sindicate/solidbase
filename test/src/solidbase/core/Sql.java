/*--
 * Copyright 2010 René M. de Bloois
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

import solidbase.core.SQLProcessor;

public class Sql
{
	@Test
	public void testSql1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "testsql1.sql" );

		processor.execute();
		processor.end();

		TestUtil.assertRecordCount( processor.getCurrentDatabase(), "USERS", 13 );
	}

	@Test
	// TODO Move to console test
	public void testSql2() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "testsql-sections.sql" );

		processor.execute();
		processor.end();

		TestUtil.assertRecordCount( processor.getCurrentDatabase(), "USERS", 13 );
	}
}
