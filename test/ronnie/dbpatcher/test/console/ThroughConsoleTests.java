package ronnie.dbpatcher.test.console;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ronnie.dbpatcher.Main;
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
		console.addAnswer( "1.0.2" );

		Main.console = console;

		Main.main( new String[] { "-verbose" } );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, LogicaCMG", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );

		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../dbpatcher-default.properties\n" +
				"Reading property file C:\\...\\dbpatcher.properties\n" +
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG\n" +
				"\n" +
				"Available database:\n" +
				"    prod (DHL Production)\n" +
				"    test (test)\n" +
				"Select a database from the above:\n" +
				"Available applications in database 'DHL Production':\n" +
				"    app1 (app1 description)\n" +
				"    app2 (app2)\n" +
				"Select an application from the above:\n" +
				"DEBUG: driverName=org.apache.derby.jdbc.EmbeddedDriver, url=jdbc:derby:c:/...;create=true, user=app\n" +
				"Connecting to database 'DHL Production', application 'app1 description'...\n" +
				"Input password for user 'app': The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch.sql'\n" +
				"Possible targets are: 1.0.1, 1.0.2\n" +
				"Input target version: Patching \"null\" to \"1.0.1\"\n" +
				"Creating table DBVERSION.\n" +
				"Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Patching \"1.0.1\" to \"1.0.2\"DEBUG: version=1.0.1, target=null, statements=2\n" +
				"Creating table USERS.\n" +
				"Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}
}
