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
import java.util.Iterator;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;


public class SqlSectionsTests extends BuildFileTest
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
	public void testSqlTask() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		configureProject( "test-sqltask.xml" );
		this.project.setBaseDir( new File( "." ) ); // Needed when testing through Maven
		executeTarget( "ant-test-sections" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Opening file 'X:\\...\\testsql-sections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"Creating table USERS...\n" +
				"Filling USERS...\n" +
				"    Inserting admin user...\n" +
				"    Inserting 3 users...\n" +
				"    Inserting 3 users...\n" +
				"Adding more USERS...\n" +
				"    Inserting 3 users...\n" +
				"And a message...\n" +
				"    Inserting 3 users...\n" +
				"Execution complete.\n" +
				"\n"
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
				SqlSectionsTests.this.logBuffer.append( event.getMessage() );
				SqlSectionsTests.this.logBuffer.append( '\n' );
			}
		}
	}
}
