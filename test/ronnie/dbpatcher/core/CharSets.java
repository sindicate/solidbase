package ronnie.dbpatcher.core;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import ronnie.dbpatcher.test.core.TestProgressListener;

public class CharSets
{
	@Test
	public void testIso8859() throws IOException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.openPatchFile( "testpatch1.sql" );
		Assert.assertEquals( Patcher.patchFile.lis.getEncoding(), "ISO-8859-1" );
	}

	@Test
	public void testUtf8() throws IOException, SQLException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testUtf8" ), "sa", null );

		Patcher.openPatchFile( "patch-utf-8-1.sql" );
		Assert.assertEquals( Patcher.patchFile.lis.getEncoding(), "UTF-8" );
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

		Connection connection = Patcher.database.getConnection( "sa" );
		Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM USERS" );
		assert result.next();
		String userName = result.getString( "USER_USERNAME" );
		Assert.assertEquals( userName, "rené" );
	}

	@Test(expectedExceptions=UnsupportedOperationException.class)
	public void testUtf16Bom() throws IOException, SQLException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.openPatchFile( "patch-utf-16-bom-1.sql" );
		//Assert.assertEquals( Patcher.patchFile.encoding, "UTF-16" );
	}
}
