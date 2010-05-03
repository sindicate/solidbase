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

package solidbase.test.downgrade;

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.Patcher;
import solidbase.core.TestUtil;
import solidbase.test.core.TestProgressListener;

public class Downgrade
{
	@Test
	public void testDowngrade() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		Patcher patcher = new Patcher( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:downgrade1", "sa", null, progress ) );

		patcher.init( "testpatch-downgrade-1.sql" );

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;

		System.out.println( "Patching to 1.1.0" );
		patcher.patch( "1.1.0" );
		TestUtil.verifyVersion( patcher, "1.1.0", null, 1, "1.1" );
		TestUtil.verifyHistoryIncludes( patcher, "1.1.0" );

		System.out.println( "Patching to 1.0.3" );
		patcher.patch( "1.0.3" );
		// TODO Why don't we get an error here?
		TestUtil.verifyVersion( patcher, "1.1.0", null, 1, "1.1" );

		System.out.println( "Patching to 1.0.3" );
		patcher.patch( "1.0.3", true );
		TestUtil.verifyVersion( patcher, "1.0.3", null, 1, "1.1" );
		TestUtil.verifyHistoryIncludes( patcher, "1.0.2" );
		TestUtil.verifyHistoryNotIncludes( patcher, "1.1.0" );

		patcher.end();
	}
}
