package ronnie.dbpatcher.test.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ronnie.dbpatcher.Main;
import ronnie.dbpatcher.core.Patcher;

public class CommandLineTests
{
	@BeforeClass
	protected void init()
	{
		Patcher.end();
	}

	@Test
	public void testCommandLine()
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:test2",
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-patchfile", "dbpatch-hsqldb-example.sql" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, LogicaCMG", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"Patching \"null\" to \"1.0.1\"\n" +
				"Creating table DBVERSION.\n" +
				"Creating table DBVERSIONLOG.\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Patching \"1.0.1\" to \"1.0.2\"DEBUG: version=1.0.1, target=null, statements=2\n" +
				"Creating table USERS.\n" +
				"Inserting admin user.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=2\n" +
				"The database has been patched.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}

	@Test
	public void testCommandLineNotPossible() throws IOException
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		Main.main( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:test2",
				"-username", "sa",
				"-password", "",
				"-target", "100.0.*",
				"-patchfile", "dbpatch-hsqldb-example.sql" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, LogicaCMG", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//output = output.replaceAll( "\\n\\s+at\\s+.+", "" );

		Assert.assertEquals( output,
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, LogicaCMG\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"\n"
		);

		String error = console.getErrorOutput();
		error = new BufferedReader( new StringReader( error ) ).readLine();

		Assert.assertNotNull( error, "Expected an error" );
		Assert.assertEquals( error, "com.logicacmg.idt.commons.SystemException: Target 100.0.* is not a possible target" );
	}
}
