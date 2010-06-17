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

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.PatchProcessor;
import solidbase.core.SQLExecutionException;
import solidbase.core.TestUtil;
import solidbase.core.UnterminatedStatementException;

public class Basic
{
	@Test
	public void testBasic() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		// TODO Learn to really shutdown an inmemory database

		patcher.init( "testpatch1.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.end();
	}

	@Test(dependsOnMethods="testBasic")
	public void testRepeat() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch1.sql" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.end();
	}

	@Test(dependsOnMethods="testRepeat")
	public void testMissingGo() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch2.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		try
		{
			patcher.patch( "1.0.3" );
			Assert.fail();
		}
		catch( SQLExecutionException e )
		{
			System.out.println( e.getMessage() );
			Assert.assertTrue( e.getMessage().contains( "unexpected token: /" ) );
		}

		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );
		patcher.end();
	}

	@Test(dependsOnMethods="testMissingGo")
	public void testDumpXML()
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch1.sql" );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		patcher.logToXML( out );
//		String xml = out.toString( "UTF-8" );
//		System.out.println( xml );

		patcher.end();
	}

	@Test(dependsOnMethods="testMissingGo")
	public void testOverrideControlTables() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-overridecontroltables.sql" );
		assert patcher.getCurrentVersion() == null;

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.1" );
		assert patcher.getCurrentVersion().equals( "1.0.1" );

		patcher.end();
	}

	@Test
	public void testOpen() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-open.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.*" );

		TestUtil.verifyVersion( patcher, "1.0.2", "1.0.3", 1, null );

		patcher.end();
	}

	@Test(expectedExceptions=UnterminatedStatementException.class)
	public void testUnterminatedCommand1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-unterminated1.sql" );
		try
		{
			Set< String > targets = patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			patcher.patch( "1.0.1" );
		}
		finally
		{
			TestUtil.verifyVersion( patcher, null, "1.0.1", 1, null );
			patcher.end();
		}
	}

	@Test(expectedExceptions=UnterminatedStatementException.class)
	public void testUnterminatedCommand2() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-unterminated2.sql" );
		try
		{
			Set< String > targets = patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			patcher.patch( "1.0.1" );
		}
		finally
		{
			TestUtil.verifyVersion( patcher, null, "1.0.1", 1, null );
			patcher.end();
		}
	}

	// TODO Create test that failes immediately and check that the target is still null

	@Test
	public void testSharedPatchBlock() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-sharedpatch1.sql" );
		try
		{
			Set< String > targets = patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			patcher.patch( "1.0.2" );
		}
		finally
		{
			patcher.closePatchFile();
		}

		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );
	}

	@Test(groups="new")
	public void testConnectionSetup() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );

		patcher.init( "testpatch-connectionsetup1.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.1" );
		TestUtil.verifyVersion( patcher, "1.0.1", null, 1, "1.1" );

		patcher.end();
	}
}
