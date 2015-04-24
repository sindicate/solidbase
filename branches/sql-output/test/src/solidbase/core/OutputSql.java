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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.PatchProcessor;

public class OutputSql
{
	static private final String db = "jdbc:hsqldb:mem:testdb";

	@Test(groups="new")
	public void testBasic() throws SQLException, IOException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );
		PatchProcessor patcher = Setup.setupPatchProcessor( "testpatch1.sql", db );
		patcher.sqlWriter = new PrintWriter( "test.sql" );
		patcher.dbVersion.sqlWriter = patcher.sqlWriter;

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.patch( "1.0.2" );

		patcher.end();
	}
}
