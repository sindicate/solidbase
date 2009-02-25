package ronnie.dbpatcher.test.digimeente;

import java.io.FileNotFoundException;
import java.io.IOException;

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ronnie.dbpatcher.Main;
import ronnie.dbpatcher.Progress;
import ronnie.dbpatcher.config.Configuration;
import ronnie.dbpatcher.config.Manipulator;
import ronnie.dbpatcher.core.Patcher;
import ronnie.dbpatcher.test.console.MockConsole;

public class DigimeenteTests
{
	@BeforeClass
	protected void init()
	{
		Patcher.end();
	}

	@Test
	public void testDigimeenteCommandLineWithClass() throws FileNotFoundException, IOException
	{
		Mockit.tearDownMocks();
		Mockit.redefineMethods( Configuration.class, new MockConfiguration( "../test/dbpatcher-digimeente.properties" ) );

		// Test the mock itself
		Configuration configuration = new Configuration( new Progress( null, false ), 2, null, null, null, null, null, null );
		Assert.assertEquals( Manipulator.getConfigurationPropertiesFile( configuration ).getName(), "dbpatcher-digimeente.properties" );

		MockConsole console = new MockConsole();
		console.addAnswer( "Zaanstad-slot1" );
		console.addAnswer( "" );
		console.addAnswer( "1.0.2" );

		Main.console = console;

		// TODO Rename patchfile to test the -patchfile option
		Main.main( "-verbose" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//		output = output.replaceAll( "\\\t", "\\t" );

		//		new FileOutputStream( new File( "dump.txt" ) ).write( output.getBytes() );
		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../dbpatcher-default.properties\n" +
				"Reading property file C:\\...\\dbpatcher-digimeente.properties\n" +
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"Available database:\n" +
				"    Zaanstad-slot1\n" +
				"    Zaanstad-slot2\n" +
				"Select a database from the above: \n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:digimeente1, user=sa\n" +
				"Connecting to database 'Zaanstad-slot1', application 'midoffice'...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Possible targets are: 1.0.1, 1.0.2\n" +
				"Input target version: Patching \"null\" to \"1.0.1\"\n" +
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
	public void testDigimeenteCommandLineWithScript() throws FileNotFoundException, IOException
	{
		Mockit.tearDownMocks();
		Mockit.redefineMethods( Configuration.class, new MockConfiguration( "../test/dbpatcher-digimeente2.properties" ) );

		MockConsole console = new MockConsole();
		console.addAnswer( "Zaanstad-slot2" );
		console.addAnswer( "" );
		console.addAnswer( "1.0.2" );

		Main.console = console;

		Main.main( "-verbose" );

		String output = console.getOutput();
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "DBPatcher v1\\.0\\.\\d+\\s+\\(C\\) 2006-200\\d R\\.M\\. de Bloois, Logica", "DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "\\\r", "" );
		//		output = output.replaceAll( "\\\t", "\\t" );

		//		new FileOutputStream( new File( "dump.txt" ) ).write( output.getBytes() );
		//		System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../dbpatcher-default.properties\n" +
				"Reading property file C:\\...\\dbpatcher-digimeente2.properties\n" +
				"DBPatcher v1.0.x (C) 2006-200x R.M. de Bloois, Logica\n" +
				"\n" +
				"Available database:\n" +
				"    Zaanstad-slot1\n" +
				"    Zaanstad-slot2\n" +
				"Select a database from the above: \n" +
				"DEBUG: driverName=org.hsqldb.jdbcDriver, url=jdbc:hsqldb:mem:digimeente2, user=sa\n" +
				"Connecting to database 'Zaanstad-slot2', application 'midoffice'...\n" +
				"Input password for user 'sa': The database has no version yet.\n" +
				"Opening patchfile 'C:\\...\\dbpatch-hsqldb-example.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Possible targets are: 1.0.1, 1.0.2\n" +
				"Input target version: Patching \"null\" to \"1.0.1\"\n" +
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
}
