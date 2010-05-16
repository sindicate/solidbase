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

package solidbase.test.init;

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.PatchProcessor;
import solidbase.core.TestUtil;
import solidbase.test.core.TestProgressListener;

public class Init
{
	@Test
	public void testInit1() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init1", "sa", null, progress ) );

		patcher.init( "testpatch1.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.1" );
		TestUtil.verifyVersion( patcher, "1.0.1", null, 2, null );

		patcher.end();
	}

	@Test(dependsOnMethods="testInit1")
	public void testInit2() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init1", "sa", null, progress ) );

		patcher.init( "testpatch-version-table-upgrade-2.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.2" );

		TestUtil.verifyVersion( patcher, "1.0.2", null, 1, "1.1.1" );
		patcher.end();
	}

	@Test
	public void testInit3() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init3", "sa", null, progress ) );

		patcher.init( "testpatch-version-table-upgrade-2.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "" );
		TestUtil.verifyVersion( patcher, null, null, 0, "1.1.1" );

		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 1, "1.1.1" );

		patcher.end();
	}
}
