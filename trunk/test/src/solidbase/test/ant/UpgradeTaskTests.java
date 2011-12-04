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
import java.util.Iterator;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;


public class UpgradeTaskTests extends BuildFileTest
{
	protected StringBuilder logBuffer;

	@Override
	public void configureProject( String filename, int logLevel )
	{
		super.configureProject( filename, logLevel );

		// The current listener that BuildFileTest uses does not add newlines with each log message.
		// Thus you cannot distinguish between separate log messages.
		// So we remove the default listener and add our own.
		// TODO Need to signal this to the Ant project

		int count = 0;
		Iterator< BuildListener > iterator = this.project.getBuildListeners().iterator();
		while( iterator.hasNext() )
		{
			BuildListener listener = iterator.next();
			if( listener.getClass().getName().equals( "org.apache.tools.ant.BuildFileTest$AntTestListener" ) )
			{
				iterator.remove();
				count++;
			}
		}
		Assert.assertEquals( count, 1 );

		this.logBuffer = new StringBuilder();
		this.project.addBuildListener( new MyAntTestListener( logLevel ) );
	}

	@Override
	public String getFullLog()
	{
		return this.logBuffer.toString();
	}

	@Override
	public String getLog()
	{
		return this.logBuffer.toString();
	}

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
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.1.0\".\n" +
				"SolidBase v1.5.x (http://solidbase.org)\n" +
				"\n" +
				"Opening file 'X:/.../testpatch-multiconnections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Current database version is \"1.1.0\".\n" +
				"Downgrading \"1.1.0\" to \"1.0.1\"\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\"\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
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
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
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

	protected class MyAntTestListener implements BuildListener
	{
		public MyAntTestListener( int logLevel )
		{
			// Not needed
		}

		public void buildStarted( BuildEvent event )
		{
			// Not needed
		}

		public void buildFinished( BuildEvent event )
		{
			// Not needed
		}

		public void targetStarted( BuildEvent event )
		{
			// Not needed
		}

		public void targetFinished( BuildEvent event )
		{
			// Not needed
		}

		public void taskStarted( BuildEvent event )
		{
			// Not needed
		}

		public void taskFinished( BuildEvent event )
		{
			// Not needed
		}

		public void messageLogged( BuildEvent event )
		{
			if( event.getPriority() == Project.MSG_INFO || event.getPriority() == Project.MSG_WARN || event.getPriority() == Project.MSG_ERR )
			{
				UpgradeTaskTests.this.logBuffer.append( event.getMessage() );
				UpgradeTaskTests.this.logBuffer.append( '\n' );
			}
		}
	}
}
