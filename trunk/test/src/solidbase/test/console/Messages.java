package solidbase.test.console;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.Main;
import solidbase.core.TestUtil;

public class Messages
{
	@Test
	public void testMessageBeforeListener() throws Exception
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		MockConsole console = new MockConsole();

		Main.console = console;

		Main.pass2( "-verbose",
				"-driver", "org.hsqldb.jdbcDriver",
				"-url", "jdbc:hsqldb:mem:testdb",
				"-username", "sa",
				"-password", "",
				"-target", "1.0.*",
				"-upgradefile", "testpatch-import1.sql" );

		String output = TestUtil.generalizeOutput( console.getOutput() );

		//System.out.println( "[[[" + output + "]]]" );

		Assert.assertEquals( output,
				"Reading property file file:/.../solidbase-default.properties\n" +
				"SolidBase v1.5.x (C) 2006-200x Rene M. de Bloois\n" +
				"\n" +
				"Opening file 'testpatch-import1.sql'\n" +
				"    Encoding is 'ISO-8859-1'\n" +
				"Connecting to database...\n" +
				"The database has no version yet.\n" +
				"Upgrading to \"1.0.1\"..\n" +
				"DEBUG: version=null, target=1.0.1, statements=2\n" +
				"Upgrading \"1.0.1\" to \"1.0.2\".\n" +
				"Starting import....\n" + // Message before listener execution
				"Generating SQLException.\n" + // There should be a dot here
				"Importing with linenumber..\n" +
				"102\n" +
				"103\n" +
				"104.\n" +
				"Importing with column list..\n" +
				"23Y\n" +
				"47Y.\n" +
				"DEBUG: version=1.0.1, target=1.0.2, statements=12\n" +
				"The database is upgraded.\n" +
				"\n" +
				"Current database version is \"1.0.2\".\n"
		);
	}
}
