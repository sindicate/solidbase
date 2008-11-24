package test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

public class Basic
{
	@Test
	public void testBasic() throws IOException, SQLException
	{
		FileUtils.deleteDirectory( new File( "c:/projects/temp/dbpatcher/db" ) );

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( new Database( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true" ), "app" );

		Patcher.openPatchFile( "testpatch1.sql" );
		try
		{
			Patcher.readPatchFile();

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
		Patcher.setConnection( new Database( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true" ), "app" );

		Patcher.openPatchFile( "testpatch2.sql" );
		try
		{
			Patcher.readPatchFile();

			List< String > targets = Patcher.getTargets();
			assert targets.size() > 0;

			Patcher.patch( "1.0.3" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}
}
