package solidbase.core.export;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.DatabaseContext;
import solidbase.core.Factory;
import solidbase.core.SQLContext;
import solidbase.core.SQLFile;
import solidbase.core.SQLProcessor;
import solidbase.core.Setup;
import solidbase.core.TestProgressListener;
import solidbase.core.TestUtil;
import solidbase.core.UpgradeProcessor;
import solidbase.util.CSVReader;
import solidstack.cbor.CBORToString;
import solidstack.io.Resources;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;

public class Export
{
	@Test
	public void testExport() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export1.sql" );
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

		SourceReader reader = SourceReaders.forResource( Resources.getResource( "export1.csv" ), "UTF-8" );
		try
		{
			CSVReader csv = new CSVReader( reader, ',', false );
			String[] line = csv.getLine();
			Assert.assertEquals( line.length, 5 );
			Assert.assertEquals( line[ 0 ], "ID" );
			Assert.assertEquals( line[ 1 ], "PICTURE" );
			Assert.assertEquals( line[ 2 ], "TEXT" );
			Assert.assertEquals( line[ 3 ], "TEXT2" );
			Assert.assertEquals( line[ 4 ], "DATE1" );
			line = csv.getLine();
			Assert.assertEquals( line.length, 5 );
			Assert.assertEquals( line[ 0 ], "1" );
			Assert.assertEquals( line[ 1 ], "000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F" );
			Assert.assertEquals( line[ 2 ], "^ Starts with a caret" );
			Assert.assertEquals( line[ 3 ], blobString );
			line = csv.getLine();
			Assert.assertEquals( line.length, 5 );
			Assert.assertEquals( line[ 0 ], "2" );
			Assert.assertEquals( line[ 1 ], "5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A5858585858585858585858585858580A" );
			Assert.assertEquals( line[ 2 ], "Does not start with a ^ caret" );
			Assert.assertEquals( line[ 3 ], "XXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\nXXXXXXXXXXXXXXX\n" );
			line = csv.getLine();
			Assert.assertNull( line );
		}
		finally
		{
			reader.close();
		}

		CBORToString toString = new CBORToString( Resources.getResource( "export3.cbor" ).newInputStream() );
		String s = toString.toString();
//		System.out.println( s );
		Assertions.assertThat( s ).isEqualTo( ""
				+ "IARRAY\n"
				+ "    UINT 01\n"
				+ "    IBSTRING\n"
				+ "        BSTRING 20: 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F\n"
				+ "    BREAK\n"
				+ "    TSTRING 15: \"^ Starts with a caret\"\n"
				+ "    ITSTRING\n"
				+ "        TSTRING 20: \"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000B\\f\\r\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F\"\n"
				+ "    BREAK\n"
				+ "    TAG 01 UINT 574A14E0\n"
				+ "BREAK\n"
				+ "IARRAY\n"
				+ "    UINT 02\n"
				+ "    IBSTRING\n"
				+ "        BSTRING 0100: 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A\n"
				+ "    BREAK\n"
				+ "    TSTRING 1D: \"Does not start with a ^ caret\"\n"
				+ "    ITSTRING\n"
				+ "        TSTRING 0100: \"XXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\n\"\n"
				+ "    BREAK\n"
				+ "    TAG 01 UINT 575743E0\n"
				+ "BREAK\n" );
	}

	@Test
	public void testExport2() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export2.sql" );
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

		CBORToString toString = new CBORToString( Resources.getResource( "export5.cbor" ).newInputStream() );
		String s = toString.toString();
		System.out.println( s );
//		Assertions.assertThat( s ).isEqualTo( ""
//				+ "IARRAY\n"
//				+ "    UINT 01\n"
//				+ "    UINT 01\n"
//				+ "    UINT 01\n"
//				+ "    UINT 01\n"
//				+ "    TSTRING 01: \"1\"\n"
//				+ "    DFLOAT 1.0\n"
//				+ "    BOOL true\n"
//				+ "    TSTRING 01: \"Y\"\n"
//				+ "    TSTRING 04: \"test\"\n"
//				+ "    ITSTRING\n"
//				+ "        TSTRING 04: \"test\"\n"
//				+ "    BREAK\n"
//				+ "    BSTRING 01: 01\n"
//				+ "    BSTRING 04: 01 02 03 04\n"
//				+ "    IBSTRING\n"
//				+ "        BSTRING 04: 01 02 03 04\n"
//				+ "    BREAK\n"
//				+ "    TAG 01 UINT 574A14E0\n"
//				+ "    TAG 01 UINT EDBA\n"
//				+ "    TAG 01 UINT 574B10AA\n"
//				+ "    TAG 01 UINT 574A14E0\n"
//				+ "    TAG 01 UINT EDBA\n"
//				+ "    TAG 01 UINT 574B10AA\n"
//				+ "BREAK\n" );

		processor.upgrade( "3" );
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
