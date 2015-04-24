/*--
 * Copyright 2011 René M. de Bloois
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

package solidbase.core.db;

import java.sql.SQLException;
import java.util.Set;

import mockit.Mockit;

import org.testng.annotations.Test;

import solidbase.core.Setup;
import solidbase.core.TestUtil;
import solidbase.core.UpgradeProcessor;
import solidbase.test.mocks.PostgreSQLDriverManager;

public class PostgreSQL
{
	static private final String db = "jdbc:hsqldb:mem:testdb2";

	@Test
	public void testTransactionAborted() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );

		// This mock DriverManager (and mock Connection, PreparedStatement) simulate PostgreSQL
		// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it
		Mockit.setUpMocks( PostgreSQLDriverManager.class );

		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "testpatch1.sql", db );

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.upgrade( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.end();
	}
}
