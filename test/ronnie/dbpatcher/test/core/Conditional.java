package ronnie.dbpatcher.test.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

public class Conditional
{
	@Test
	public void testIfHistoryContains() throws IOException, SQLException
	{
		Patcher.end();

		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testBasic" ), "sa", null );

		Patcher.openPatchFile( "testpatch3.sql" );
		try
		{
			Set< String > targets = Patcher.getTargets( false, null );
			assert targets.size() > 0;

			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}
	}
}
