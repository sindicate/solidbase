package ronnie.dbpatcher.test.core;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

public class Basic
{
	@Test
	public void testBasic() throws IOException, SQLException
	{
		Patcher.end();
		FileUtils.deleteDirectory( new File( "c:/projects/temp/dbpatcher/db" ) );

		Patcher.setCallBack( new TestProgressListener() );
		// TODO Learn to really shutdown an inmemory database
		Patcher.setConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3" ), "sa", null );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}

	@Test(dependsOnMethods="testBasic", expectedExceptions=SQLException.class)
	public void testMissingGo() throws IOException, SQLException
	{
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test3" ), "sa", null );

		Patcher.openPatchFile( "testpatch2.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false );
			assert targets.size() > 0;

			Patcher.patch( "1.0.3" );
		}
		finally
		{
			Patcher.end();
		}
	}
}
