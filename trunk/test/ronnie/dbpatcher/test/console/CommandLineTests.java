package ronnie.dbpatcher.test.console;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.logicacmg.idt.commons.SystemException;

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
	public void testCommandLine() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main0( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:test2",
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-patchfile", "dbpatch-hsqldb-example.sql" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
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
	public void testCommandLineNotPossible() throws Exception
	{
		MockConsole console = new MockConsole();

		Main.console = console;

		try
		{
			Main.main0( "-verbose",
					"-driver", "org.hsqldb.jdbcDriver",
					"-url", "jdbc:hsqldb:mem:test2",
					"-username", "sa",
					"-password", "",
					"-target", "100.0.*",
					"-patchfile", "dbpatch-hsqldb-example.sql" );

			Assert.fail( "Expected a SystemException" );
		}
		catch( SystemException e )
		{
			Assert.assertEquals( e.getMessage(), "Target 100.0.* is not a possible target" );
		}

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//output = output.replaceAll( "\\n\\s+at\\s+.+", "" );

		Assert.assertEquals( output,
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:test2, user=sa\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n"
		);
	}
}
