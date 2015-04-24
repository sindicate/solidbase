package solidbase.core;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.testng.annotations.Test;

import solidstack.io.Resources;

public class Export
{
	@Test
	public void testExport() throws SQLException, UnsupportedEncodingException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export1.sql" );
		processor.upgrade( "1" );

		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 ( ID, PICTURE, TEXT, TEXT2, DATE1 ) VALUES ( ?, ?, ?, ?, ? )" );

		byte[] blob = new byte[ 32 ];
		for( int i = 0; i < blob.length; i++ )
			blob[ i ] = (byte)i;
		statement.setInt( 1, 1 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.setString( 3, "^ Starts with a caret" );
		statement.setString( 4, new String( blob, "ISO-8859-1" ) );
		statement.setTimestamp( 5, new Timestamp( System.currentTimeMillis() ) );
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
		statement.setTimestamp( 5, new Timestamp( System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 10 ) );
		statement.execute();

		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		processor.end();
	}

	@Test(enabled=false)
	public void testExportOracle() throws SQLException, UnsupportedEncodingException
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@192.168.0.109:1521:XE", "XXXX", "XXXX", progress );
		SQLProcessor processor = new SQLProcessor( progress );
		SQLFile sqlFile = Factory.openSQLFile( Resources.getResource( "testsql-export-oracle.sql" ), progress );
		DatabaseContext databases = new DatabaseContext( database );
		SQLContext context = new SQLContext( sqlFile.getSource() );
		context.setDatabases( databases );
		processor.setContext( context );

		processor.process();
		processor.end();
	}
}
