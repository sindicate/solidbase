package solidbase.core;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.testng.annotations.Test;

public class Export
{
	@Test(groups="new")
	public void testExport() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export1.sql" );
		processor.upgrade( "1" );

		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 VALUES ( ?, ? )" );
		byte[] blob = "Dit is een blob".getBytes();
		statement.setInt( 1, 1 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.execute();
		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		processor.end();
	}
}
