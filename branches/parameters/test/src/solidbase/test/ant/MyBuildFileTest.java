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

import java.util.Iterator;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.testng.Assert;


public class MyBuildFileTest extends BuildFileTest
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
				MyBuildFileTest.this.logBuffer.append( event.getMessage() );
				MyBuildFileTest.this.logBuffer.append( '\n' );
			}
		}
	}
}
