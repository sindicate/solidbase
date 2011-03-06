package solidbase.core;

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

public class ClassPathTest
{
	static private final String db = "jdbc:hsqldb:mem:testdb";

	@Test(groups="new")
	public void testClassPath() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( db, "sa", null );
		PatchProcessor patcher = Setup.setupPatchProcessor( "classpath:testpatch-classpath.sql", db );

		Set< String > targets = patcher.getTargets( false, null, false );
		assert targets.size() > 0;
		patcher.patch( "1.0.2" );
		TestUtil.verifyVersion( patcher, "1.0.2", null, 2, null );

		patcher.end();
	}
}
