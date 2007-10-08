package test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import ronnie.dbpatcher.core.Patcher;

public class Basic
{
	@Test
	public void test() throws IOException, SQLException
	{
		FileUtils.deleteDirectory( new File( "c:/projects/temp/dbpatcher/db" ) );
		
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:c:/projects/temp/dbpatcher/db;create=true", "app" );
		Patcher.setPatchFileName( "testpatch1.sql" );
		
		Patcher.openPatchFile();
		Patcher.readPatchFile();
		
		List< String > targets = Patcher.getTargets();
		assert targets.size() > 0;
		
		Patcher.patch( "1.0.2" );
		
		Patcher.end();
	}
}
