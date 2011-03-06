package solidbase.core;

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.util.DriverDataSource;
import solidbase.util.FileResource;

public class DataSourceTests
{
	@Test
	public void testWithDataSource() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		DriverDataSource dataSource = new DriverDataSource( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb2", "sa", null );
		Database database = new Database( "default", dataSource, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );
		PatchFile patchFile = Factory.openPatchFile( new FileResource( "testpatch1.sql" ), progress );
		processor.setPatchFile( patchFile );
		processor.init();

		Set< String > targets = processor.getTargets( false, null, false );
		assert targets.size() > 0;
		processor.patch( "1.0.2" );
		TestUtil.verifyVersion( processor, "1.0.2", null, 2, null );

		processor.end();
	}

	@Test
	public void testWithDataSourceAndUser() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		DriverDataSource dataSource = new DriverDataSource( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb2", "sa", null );
		Database database = new Database( "default", dataSource, "sa", null, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );
		PatchFile patchFile = Factory.openPatchFile( new FileResource( "testpatch1.sql" ), progress );
		processor.setPatchFile( patchFile );
		processor.init();

		Set< String > targets = processor.getTargets( false, null, false );
		assert targets.size() > 0;
		processor.patch( "1.0.2" );
		TestUtil.verifyVersion( processor, "1.0.2", null, 2, null );

		processor.end();
	}
}
