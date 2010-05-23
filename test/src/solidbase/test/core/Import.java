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

package solidbase.test.core;

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.TestUtil;
import solidbase.core.PatchProcessor;

public class Import
{
	@Test
	public void testImport() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress );
		PatchProcessor patcher = new PatchProcessor( progress, database );

		patcher.init( "testpatch-import1.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.2" );

		TestUtil.verifyVersion( patcher, "1.0.2", null, 12, null );
		TestUtil.assertRecordCount( database, "TEMP", 7 );

		patcher.end();
	}
}
