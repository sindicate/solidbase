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

package solidbase.test.mocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import mockit.Mock;
import mockit.MockClass;

// This mock DriverManager (and mock Connection, PreparedStatement) simulate PostgreSQL
// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it
@MockClass(realClass=DriverManager.class)
public class PostgreSQLDriverManager
{
	@Mock(reentrant=true)
	static public Connection getConnection( String url, String user, String password ) throws SQLException
	{
		Connection result = DriverManager.getConnection( url, user, password );
		return new PostgreSQLConnection( result );
	}
}
