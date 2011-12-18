package solidbase.core;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.testng.annotations.Test;

public class Export
{
	@Test
	public void testExport() throws SQLException, UnsupportedEncodingException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export1.sql" );
		processor.upgrade( "1" );

		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 ( ID, PICTURE, TEXT, TEXT2 ) VALUES ( ?, ?, ?, ? )" );

		byte[] blob = "Dit is een blob".getBytes();
		statement.setInt( 1, 1 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.setString( 3, "^ Starts with a caret" );
		statement.setString( 4, "Dit is een CLOB" );
		statement.execute();

		blob = new byte[ 256 ];
		for( int i = 0; i < 256; i++ )
			if( ( i + 1 ) % 16 == 0 )
				blob[ i ] = '\n';
			else
				blob[ i ] = 'X';
		statement.setInt( 1, 2 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.setString( 3, "Does not start with a ^ caret" );
		statement.setString( 4, new String( blob, "ISO-8859-1" ) );
		statement.execute();

		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		processor.end();
	}
}
