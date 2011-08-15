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
import java.util.Set;

import org.testng.annotations.Test;

public class Crm
{
	@Test
	public void testCRM() throws SQLException
	{
		TestUtil.dropDerbyDatabase( "jdbc:derby:memory:test" );

		UpgradeProcessor patcher = Setup.setupDerbyUpgradeProcessor( "testpatch-crm.sql" );
		patcher.init();

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.upgrade( (String)null );
		//TestUtil.verifyVersion( patcher, "1.0.2", null, 1, "1.1" );

		patcher.end();
	}
}
