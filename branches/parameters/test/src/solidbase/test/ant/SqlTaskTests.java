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

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;


@SuppressWarnings( "javadoc" )
public class SqlTaskTests extends MyBuildFileTest
{
	@Test
	public void testSqlTask()
	{
		configureProject( "test-sqltask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Creating table USERS...\n" +
				"Inserting admin user...\n" +
				"Inserting 3 users...\n" +
				"Inserting 3 users...\n" +
				"Inserting 3 users...\n" +
				"Inserting 3 users...\n" +
				"Opening file 'X:/.../testsql2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Inserting 3 users...\n" +
				"Execution complete.\n" +
				"\n"
				);
	}

	@Test
	public void testSqlFileDoesNotExist()
	{
		String log = TestUtil.captureAnt( new Runnable()
		{
			public void run()
			{
				new AntMain().startAnt( new String[] { "-f", "test-sqltask.xml", "ant-test-filenotfound" }, null, null );
			}
		} );
		log = TestUtil.generalizeOutput( log );
		Assert.assertEquals( log, "Buildfile: test-sqltask.xml\n" +
				"\n" +
				"ant-test-filenotfound:\n" +
				"   [sb-sql] SolidBase v1.5.x (http://solidbase.org)\n" +
				"   [sb-sql] \n" +
				"   [sb-sql] Opening file 'X:/.../doesnotexist.sql'\n" +
				"   [sb-sql] Execution aborted.\n" +
				"\n" +
				"BUILD FAILED\n" +
				"X:/.../test-sqltask.xml:47: java.io.FileNotFoundException: X:/.../doesnotexist.sql (The system cannot find the file specified)\n" +
				"\n" +
				"Total time: 0 seconds\n"
				);
	}

	@Test
	public void testSqlParameters()
	{
		configureProject( "test-sqltask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test-parameters" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testsql-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"val1\n" +
				"Execution complete.\n" +
				"\n"
				);
	}
}
