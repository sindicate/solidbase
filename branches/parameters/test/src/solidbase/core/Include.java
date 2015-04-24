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

public class Include
{
	static private final String db = "jdbc:hsqldb:mem:testdb2";

	@Test
	public void testInclude() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-include1.sql", db );

		Set< String > targets = processor.getTargets( false, null, false );
		assert targets.size() > 0;
		processor.upgrade( "1.0.1" );
		//TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		processor.end();
	}
}
