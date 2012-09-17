/*--
 * Copyright 2009 René M. de Bloois
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
public class UpgradeTaskTests extends MyBuildFileTest
{
	@Test
	public void testUpgradeTask()
	{
		configureProject( "test-upgradetask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-multiconnections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Setting up control tables to \"1.1\"\n" +
				"    Creating table DBVERSION...\n" +
				"    Creating table DBVERSIONLOG...\n" +
				"Upgrading to \"1.0.1\"\n" +
				"Upgrading \"1.0.1\" to \"1.1.0\"\n" +
				"    Inserting admin users...\n" +
				"\n" +
				"Current database version is \"1.1.0\".\n" +
				"Upgrade complete.\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-multiconnections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Current database version is \"1.1.0\".\n" +
				"Downgrading \"1.1.0\" to \"1.0.1\"\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\"\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrade complete.\n"
				);
	}

	@Test
	public void testUpgradeTaskBaseDir()
	{
		configureProject( "test-upgradetask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-basedir-test" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-basedir.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating table DBVERSION...\n" +
				"    Creating table DBVERSIONLOG...\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\"\n" +
				"    Creating table USERS...\n" +
				"    Inserting admin user...\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n" +
				"Upgrade complete.\n"
				);
	}

	@Test
	public void testUpgradeFileDoesNotExist()
	{
		String log = TestUtil.captureAnt( new Runnable()
		{
			public void run()
			{
				new AntMain().startAnt( new String[] { "-f", "test-upgradetask.xml", "ant-test-filenotfound" }, null, null );
			}
		} );
		log = TestUtil.generalizeOutput( log );
		Assert.assertEquals( log, "Buildfile: test-upgradetask.xml\n" +
				"\n" +
				"ant-test-filenotfound:\n" +
				"[solidbase-upgrade] SolidBase v1.5.x (http://solidbase.org)\n" +
				"[solidbase-upgrade] \n" +
				"[solidbase-upgrade] Opening file 'X:/.../doesnotexist.sql'\n" +
				"\n" +
				"BUILD FAILED\n" +
				"X:/.../test-upgradetask.xml:51: java.io.FileNotFoundException: X:/.../doesnotexist.sql (The system cannot find the file specified)\n" +
				"\n" +
				"Total time: 0 seconds\n"
				);
	}

	@Test
	public void testUpgradeParameters()
	{
		configureProject( "test-upgradetask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test-parameters" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-parameter2.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database is unmanaged.\n" +
				"Setting up control tables to \"1.1\"\n" +
				"Opening file 'X:/.../setup-1.1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Upgrading to \"1\"\n" +
				"val1\n" +
				"\n" +
				"Current database version is \"1\".\n" +
				"Upgrade complete.\n"
				);
	}
}
