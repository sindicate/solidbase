/*--
 * Copyright 2011 René M. de Bloois
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

package solidbase.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import solidbase.core.Setup;
import solidbase.core.TestUtil;
import solidbase.core.UpgradeProcessor;
import solidbase.test.mocks.PostgreSQLConnection;


@SuppressWarnings( "javadoc" )
public class PostgreSQL
{
	// See https://www.baeldung.com/jmockit-static-method

	@SuppressWarnings( "unused" )
	@Test
	public void testTransactionAborted() throws SQLException {

		// This mock DriverManager (and mock Connection, PreparedStatement) simulate PostgreSQL,
		// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it.
		new MockUp<DriverManager>() {
			@Mock
	    	public Connection getConnection( Invocation invocation, String url, String user, String password ) throws SQLException {
	    		Connection result = invocation.proceed( url, user, password );
	    		return new PostgreSQLConnection( result );
	    	}
	    };

		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );

		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "testpatch1.sql", Setup.defaultdb );

		Set<String> targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.upgrade( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.end();
	}

}
