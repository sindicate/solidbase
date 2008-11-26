package ronnie.dbpatcher.test.console;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ronnie.dbpatcher.Progress;
import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

public class ThroughConsoleTests
{
	@BeforeClass
	protected void init() throws IOException
	{
		Patcher.end();
		try
		{
			DriverManager.getConnection( "jdbc:derby:c:/projects/temp/dbpatcher/db;shutdown=true" );
		}
		catch( SQLException e )
		{
			assert e.getSQLState().equals( "08006" ) || e.getSQLState().equals( "XJ004" ) : "Did not expect " + e.getSQLState() + ": " + e.getMessage();
		}
		FileUtils.deleteDirectory( new File( "c:/projects/temp/dbpatcher/db" ) );
	}

	@Test
	public void testConsole() throws IOException, SQLException
	{
		MockConsole console = new MockConsole();
		console.addAnswer( "prod" );
		console.addAnswer( "app1" );
		console.addAnswer( "" );
		console.addAnswer( "1.0.1" );

		Patcher.setCallBack( new Progress( console, true ) );
		Patcher.setConnection( new Database( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true" ), "app", null );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			List< String > targets = Patcher.getTargets();
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );

			Assert.assertEquals( console.getOutput().replaceAll( "\\\r", "" ),
					"DEBUG: driverName=org.apache.derby.jdbc.EmbeddedDriver, url=jdbc:derby:c:/projects/temp/dbpatcher/db;create=true, user=app\n" +
					"Opening patchfile 'file:/C:/PROJECTS/BUILDS/dbpatcher/test/testpatch1.sql'\n" +
					"Input password for user 'app': Patching \"null\" to \"1.0.1\"\n" +
					"Creating table DBVERSION.\n" +
					"Creating table DBVERSIONLOG.\n" +
					"DEBUG: version=null, target=1.0.1, statements=2\n" +
					"Patching \"1.0.1\" to \"1.0.2\"DEBUG: version=1.0.1, target=null, statements=2\n" +
					"Creating table USERS.\n" +
					"Inserting admin user.\n" +
					"DEBUG: version=1.0.1, target=1.0.2, statements=2\n"
			);
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}
}
