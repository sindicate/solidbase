package solidbase.test.ant;

import java.util.Iterator;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.test.TestUtil;


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

	@Test(groups="new")
	public void testUpgradeTask()
	{
		configureProject( "test-upgradetask.xml" );
		executeTarget( "ant-test" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x\n" +
				"(C) 2006-2009 Rene M. de Bloois\n" +
				"\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening file 'file:/.../testpatch-multiconnections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating table DBVERSION.\n" +
				"    Creating table DBVERSIONLOG.\n" +
				"Upgrading \"1.0.1\" to \"1.1.0\".\n" +
				"    Inserting admin users...\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.1.0\".\n" +
				"SolidBase v1.5.x\n" +
				"(C) 2006-2009 Rene M. de Bloois\n" +
				"\n" +
				"Connecting to database...\n" +
				"Current database version is \"1.1.0\".\n" +
				"Opening file 'file:/.../testpatch-multiconnections.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
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
		executeTarget( "ant-basedir-test" );
		String log = TestUtil.generalizeOutput( getLog() );
		Assert.assertEquals( log, "SolidBase v1.5.x\n" +
				"(C) 2006-2009 Rene M. de Bloois\n" +
				"\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening file 'X:\\...\\testpatch-basedir.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Upgrading to \"1.0.1\"\n" +
				"    Creating table DBVERSION.\n" +
				"    Creating table DBVERSIONLOG.\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\"\n" +
				"    Creating table USERS.\n" +
				"    Inserting admin user.\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}

	protected class MyAntTestListener implements BuildListener
	{
		private int logLevel;

		public MyAntTestListener( int logLevel )
		{
			this.logLevel = logLevel;
		}

		public void buildStarted( BuildEvent event )
		{
		}

		public void buildFinished( BuildEvent event )
		{
		}

		public void targetStarted( BuildEvent event )
		{
		}

		public void targetFinished( BuildEvent event )
		{
		}

		public void taskStarted( BuildEvent event )
		{
		}

		public void taskFinished( BuildEvent event )
		{
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
