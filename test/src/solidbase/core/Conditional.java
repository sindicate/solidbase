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

import solidbase.core.UpgradeProcessor;

public class Conditional
{
	@Test
	public void testIfHistoryContains1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		UpgradeProcessor patcher = Setup.setupPatchProcessor( "testpatch-conditional1.sql" );

		patcher.upgrade( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null ); // TODO STATEMENTS should be 3.

		patcher.end();
	}

	@Test(dependsOnMethods="testIfHistoryContains1")
	public void testIfHistoryContains2() throws SQLException
	{
		UpgradeProcessor patcher = Setup.setupPatchProcessor( "testpatch-conditional2.sql" );

		patcher.upgrade( "1.0.3" );
		TestUtil.verifyVersion( patcher, "1.0.3", null, 4, "1.1" );

		patcher.end();
	}
}
