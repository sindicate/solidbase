package solidbase.test.core;

import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.PatchProcessor;
import solidbase.core.TestUtil;

public class DeprecatedVersion
{
	@Test
	public void testDeprecated1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		patcher.init( "testpatch1.sql" );
		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );
		patcher.end();
	}

	@Test(dependsOnMethods="testDeprecated1")
	public void testDeprecated2() throws SQLException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress ) );
		patcher.init( "testpatch-deprecated-version-1.sql" );
		try
		{
			patcher.patch( "1.0.1" );
			Assert.fail( "Expected a FatalException" );
		}
		catch( FatalException e )
		{
			Assert.assertTrue( e.getMessage().contains( "The current database version (1.0.2) is not available in the upgrade file." ) );
			patcher.end();
		}
	}
}
