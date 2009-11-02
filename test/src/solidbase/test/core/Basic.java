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

import java.io.IOException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.NonTerminatedStatementException;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;

public class Basic
{
	@Test
	public void testBasic() throws IOException, SQLExecutionException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		// TODO Learn to really shutdown an inmemory database
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null ) );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}

	@Test(dependsOnMethods="testBasic", expectedExceptions=SQLExecutionException.class)
	public void testMissingGo() throws IOException, SQLExecutionException
	{
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3", "sa", null ) );

		Patcher.openPatchFile( "testpatch2.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.3" );
		}
		finally
		{
			Patcher.end();
		}
	}

	@Test
	public void testOpen() throws IOException, SQLExecutionException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		// TODO Learn to really shutdown an inmemory database
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testopen", "sa", null ) );

		Patcher.openPatchFile( "testpatch-open.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}

	@Test(expectedExceptions=NonTerminatedStatementException.class)
	public void testUnterminatedCommand1() throws IOException, SQLExecutionException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testunterminated1", "sa", null ) );

		Patcher.openPatchFile( "testpatch-unterminated1.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.1" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}

	@Test(expectedExceptions=NonTerminatedStatementException.class)
	public void testUnterminatedCommand2() throws IOException, SQLExecutionException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testunterminated2", "sa", null ) );

		Patcher.openPatchFile( "testpatch-unterminated2.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.1" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}

	@Test
	public void testSharedPatchBlock() throws IOException, SQLExecutionException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testshared1", "sa", null ) );

		Patcher.openPatchFile( "testpatch-sharedpatch1.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}
}
