package solidbase.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.testng.annotations.Test;

import solidbase.util.CSVReader;
import solidstack.io.Resources;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;

public class ExportCSV
{
	@Test
	public void testExport() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export-csv1.sql" );
		processor.upgrade( "1" );

		long now = 1464537258635L;

		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 ( ID, PICTURE, TEXT, TEXT2, DATE1 ) VALUES ( ?, ?, ?, ?, ? )" );

		byte[] blob = new byte[ 32 ];
		for( int i = 0; i < blob.length; i++ )
			blob[ i ] = (byte)i;
		String blobString = new String( blob, "ISO-8859-1" );
		statement.setInt( 1, 1 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.setString( 3, "^ Starts with a caret" );
		statement.setString( 4, blobString );
		statement.setTimestamp( 5, new Timestamp( now ) );
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
		statement.setTimestamp( 5, new Timestamp( now + 1000 * 60 * 60 * 24 * 10 ) );
		statement.execute();

		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		processor.end();

		SourceReader reader = SourceReaders.forResource( Resources.getResource( "export11.csv" ), "UTF-8" );
		try
		{
			CSVReader csv = new CSVReader( reader, ',', true, false );
			String[] line = csv.getLine();
			assertThat( line.length ).isEqualTo( 5 );
			assertThat( line[ 0 ] ).isEqualTo( "ID" );
			assertThat( line[ 1 ] ).isEqualTo( "PICTURE" );
			assertThat( line[ 2 ] ).isEqualTo( "TEXT" );
			assertThat( line[ 3 ] ).isEqualTo( "TEXT2" );
			assertThat( line[ 4 ] ).isEqualTo( "DATE1" );
			line = csv.getLine();
			assertThat( line.length ).isEqualTo( 5 );
			assertThat( line[ 0 ] ).isEqualTo( "1" );
			assertThat( line[ 1 ] ).isEqualTo( "000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F" );
			assertThat( line[ 2 ] ).isEqualTo( "^ Starts with a caret" );
			assertThat( line[ 3 ] ).isEqualTo( blobString );
			line = csv.getLine();
			assertThat( line.length ).isEqualTo( 5 );
			assertThat( line[ 0 ] ).isEqualTo( "2" );
			assertThat( line[ 1 ] ).isEqualTo( "5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A" );
			assertThat( line[ 2 ] ).isEqualTo( "Does not start with a ^ caret" );
			assertThat( line[ 3 ] ).isEqualTo( "XXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\n" );
			line = csv.getLine();
			assertThat( line ).isNull();
		}
		finally
		{
			reader.close();
		}
	}

	@Test
	public void testExport2() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export-csv2.sql" );
		processor.upgrade( "1" );

		PreparedStatement statement = processor.prepareStatement( ""
				+ "INSERT INTO TEMP1 ( TINYINT, SMALLINT, INTEGER, BIGINT, DECIMAL, FLOAT, BOOLEAN, "
				+ "CHAR, VARCHAR, CLOB, BINARY, VARBINARY, BLOB,"
				+ "DATE, TIME, TIMESTAMP, DATEZ, TIMEZ, TIMESTAMPZ ) "
				+ "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )" );
		int i = 1;
		statement.setInt( i++, 1 );
		statement.setInt( i++, 1 );
		statement.setInt( i++, 1 );
		statement.setInt( i++, 1 );
		statement.setInt( i++, 1 );
		statement.setInt( i++, 1 );
		statement.setBoolean( i++, true );
		statement.setString( i++, "Y" );
		statement.setString( i++, "test" );
		statement.setString( i++, "test" );
		statement.setBytes( i++, new byte[] { 1 } );
		statement.setBytes( i++, new byte[] { 1, 2, 3, 4 } );
		statement.setBytes( i++, new byte[] { 1, 2, 3, 4 } );
		long now = 1464537258635L;
		statement.setDate( i++, new java.sql.Date( now ) );
		statement.setTime( i++, new java.sql.Time( now ) );
		statement.setTimestamp( i++, new java.sql.Timestamp( now ) );
		statement.setDate( i++, new java.sql.Date( now ) );
		statement.setTime( i++, new java.sql.Time( now ) );
		statement.setTimestamp( i++, new java.sql.Timestamp( now ) );
		statement.execute();

		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		// TODO Implement check

		processor.upgrade( "3" );

		statement = processor.prepareStatement( "SELECT * FROM TEMP1" );
		ResultSet result = statement.executeQuery();
		assertThat( result.next() ).isTrue();
		assertThat( result.getBytes( 11 ) ).containsExactly( (byte)1 );
		assertThat( result.getBytes( 12 ) ).containsExactly( (byte)1, (byte)2, (byte)3, (byte)4 );
		assertThat( result.next() ).isTrue();
		assertThat( result.getBytes( 11 ) ).containsExactly( (byte)1 );
		assertThat( result.getBytes( 12 ) ).containsExactly( (byte)1, (byte)2, (byte)3, (byte)4 );
		assertThat( result.next() ).isFalse();

		processor.end();
	}
}
