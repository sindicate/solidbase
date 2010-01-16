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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.Patcher;
import solidbase.core.TestUtil;
import solidbase.test.core.TestProgressListener;

public class Init
{
	@Test
	public void testInit1() throws IOException, SQLException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init1", "sa", null ) );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			Patcher.patch( "1.0.1" );
		}
		finally
		{
			Patcher.closePatchFile();
		}

		TestUtil.verifyVersion( "1.0.1", null, 2, null );
	}

	@Test(dependsOnMethods="testInit1")
	public void testInit2() throws IOException, SQLException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init1", "sa", null ) );

		Patcher.openPatchFile( "testpatch-version-table-upgrade-2.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}

		TestUtil.verifyVersion( "1.0.2", null, 1, "1.1.1" );
	}

	@Test
	public void testInit3() throws IOException, SQLException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:init3", "sa", null ) );

		Patcher.openPatchFile( "testpatch-version-table-upgrade-2.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null, false );
			assert targets.size() > 0;

			Patcher.patch( "" );
			TestUtil.verifyVersion( null, null, 0, "1.1.1" );

			Patcher.patch( "1.0.2" );
			TestUtil.verifyVersion( "1.0.2", null, 1, "1.1.1" );
		}
		finally
		{
			Patcher.closePatchFile();
		}

	}
}
