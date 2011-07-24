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

import solidbase.core.Database;
import solidbase.core.PatchFile;
import solidbase.core.PatchProcessor;
import solidbase.core.Factory;
import solidbase.util.FileResource;

public class Init
{
	@Test
	public void testInit1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		PatchFile patchFile = Factory.openPatchFile( new FileResource( "testpatch1.sql" ), progress );
		patcher.setPatchFile( patchFile );
		patcher.init();

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
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		PatchFile patchFile = Factory.openPatchFile( new FileResource( "testpatch-version-table-upgrade-2.sql" ), progress );
		patcher.setPatchFile( patchFile );
		patcher.init();

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 1, "1.1.1" );

		patcher.end();
	}

	@Test
	public void testInit3() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		PatchFile patchFile = Factory.openPatchFile( new FileResource( "testpatch-version-table-upgrade-2.sql" ), progress );
		patcher.setPatchFile( patchFile );
		patcher.init();

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 1, "1.1.1" );

		patcher.end();
	}
}
