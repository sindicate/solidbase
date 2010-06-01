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
import solidbase.core.UnterminatedStatementException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;
import solidbase.core.TestUtil;

public class Basic
{
	@Test
	public void testBasic() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null, progress ) );
		// TODO Learn to really shutdown an inmemory database

		patcher.openPatchFile( "testpatch1.sql" );
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
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null, progress ) );

		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.openPatchFile( "testpatch1.sql" );
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
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null, progress ) );

		patcher.openPatchFile( "testpatch2.sql" );
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
			Assert.assertTrue( e.getMessage().contains( "Unexpected token: / in statement [/]" ) );
		}

		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );
		patcher.end();
	}

	@Test(dependsOnMethods="testMissingGo")
	public void testDumpXML ()
	{
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null, progress ) );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		patcher.logToXML( out );
//		String xml = out.toString( "UTF-8" );
//		System.out.println( xml );

		patcher.end();
	}

	@Test
	public void testOpen() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testopen", "sa", null, progress ) );

		patcher.openPatchFile( "testpatch-open.sql" );
		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		patcher.patch( "1.0.2" );

		TestUtil.verifyVersion( patcher, "1.0.1", "1.0.2", 2, null );

		patcher.end();
	}

	@Test(expectedExceptions=UnterminatedStatementException.class)
	public void testUnterminatedCommand1() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testunterminated1", "sa", null, progress ) );

		patcher.openPatchFile( "testpatch-unterminated1.sql" );
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
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testunterminated2", "sa", null, progress ) );

		patcher.openPatchFile( "testpatch-unterminated2.sql" );
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
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testshared1", "sa", null, progress ) );

		patcher.openPatchFile( "testpatch-sharedpatch1.sql" );
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
}
