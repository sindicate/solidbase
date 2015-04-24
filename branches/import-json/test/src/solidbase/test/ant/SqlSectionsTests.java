/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.test.ant;

import java.io.File;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;


public class SqlSectionsTests extends MyBuildFileTest
{
	@Test
	public void testSqlTask() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		configureProject( "test-sqltask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test-sections" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql-sections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Creating table USERS...\n" +
				"Filling USERS...\n" +
				"    Inserting admin user...\n" +
				"    Inserting 3 users...\n" +
				"    Inserting 3 users...\n" +
				"Adding more USERS...\n" +
				"    Inserting 3 users...\n" +
				"    Inserting 3 users...\n" +
				"Execution complete.\n" +
				"\n"
		);
	}
}
