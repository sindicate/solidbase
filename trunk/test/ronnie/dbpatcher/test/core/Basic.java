package ronnie.dbpatcher.test.core;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
		Patcher.setConnection( new Database( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true" ), "app", null );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			List< String > targets = Patcher.getTargets();
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
		Patcher.setConnection( new Database( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true" ), "app", null );

		Patcher.openPatchFile( "testpatch2.sql" );
		try
		{
			List< String > targets = Patcher.getTargets();
			assert targets.size() > 0;

			Patcher.patch( "1.0.3" );
		}
		finally
		{
			Patcher.end();
		}
	}
}
