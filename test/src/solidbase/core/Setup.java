package solidbase.core;

import solidbase.core.Database;
import solidbase.core.PatchFile;
import solidbase.core.PatchProcessor;
import solidbase.core.SQLFile;
import solidbase.core.SQLProcessor;
import solidbase.core.Util;

public class Setup
{
	static public PatchProcessor setupPatchProcessor( String fileName, String url )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", url, "sa", null, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );
		PatchFile patchFile = Util.openPatchFile( fileName, progress );
		processor.setPatchFile( patchFile );
		processor.init();
		return processor;
	}

	static public PatchProcessor setupPatchProcessor( String fileName )
	{
		return setupPatchProcessor( fileName, "jdbc:hsqldb:mem:testdb" );
	}

	static public SQLProcessor setupSQLProcessor( String fileName )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress );
		SQLProcessor processor = new SQLProcessor( progress, database );
		SQLFile sqlFile = Util.openSQLFile( fileName, progress );
		processor.setSQLSource( sqlFile.getSource() );
		return processor;
	}
}
