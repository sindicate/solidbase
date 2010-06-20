package solidbase.test.core;

import solidbase.core.Database;
import solidbase.core.PatchFile;
import solidbase.core.PatchProcessor;
import solidbase.core.SQLFile;
import solidbase.core.SQLProcessor;
import solidbase.core.Util;

public class Setup
{
	static public PatchProcessor setupPatchProcessor( String fileName )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );
		PatchFile patchFile = Util.openPatchFile( fileName, progress );
		processor.setPatchFile( patchFile );
		processor.init();
		return processor;
	}

	static public SQLProcessor setupSQLProcessor( String fileName )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress );
		SQLProcessor processor = new SQLProcessor( progress, database );
		SQLFile sqlFile = Util.openSQLFile( "testsql1.sql", progress );
		processor.setSQLSource( sqlFile.getSource() );
		return processor;
	}
}
