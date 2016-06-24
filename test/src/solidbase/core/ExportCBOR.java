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

import solidstack.cbor.CBORToString;
import solidstack.io.Resources;

public class ExportCBOR
{
	@Test
	public void testExport() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export-cbor1.sql" );
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

		CBORToString toString = new CBORToString( Resources.getResource( "export14.cbor" ).newInputStream() );
		String s = toString.toString();
		System.out.println( s );
		assertThat( s ).isEqualTo( "MAP 3\n"
			+ "    TEXT 7: \"version\"\n"
			+ "    UINT 1\n"
			+ "    TEXT 11: \"description\"\n"
			+ "    TEXT 29: \"SolidBase CBOR Data Dump File\"\n"
			+ "    TEXT 9: \"createdBy\"\n"
			+ "    MAP 2\n"
			+ "        TEXT 7: \"product\"\n"
			+ "        TEXT 9: \"SolidBase\"\n"
			+ "        TEXT 7: \"version\"\n"
			+ "        TEXT 5: \"2.0.0\"\n"
			+ "TAG 0x0100 MAP 1\n"
			+ "    TEXT 6: \"fields\"\n"
			+ "    ARRAY 5\n"
			+ "        MAP 4\n"
			+ "            TEXT 10: \"schemaName\"\n"
			+ "            TEXT 6: \"PUBLIC\"\n"
			+ "            TEXT 9: \"tableName\"\n"
			+ "            TEXT 5: \"TEMP1\"\n"
			+ "            TEXT 4: \"name\"\n"
			+ "            TEXT 2: \"ID\"\n"
			+ "            TEXT 4: \"type\"\n"
			+ "            TEXT 7: \"INTEGER\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 7: \"PICTURE\"\n"
			+ "            TAG 0x19 UINT 6: \"type\"\n"
			+ "            TEXT 4: \"BLOB\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"TEXT\"\n"
			+ "            TAG 0x19 UINT 6: \"type\"\n"
			+ "            TEXT 7: \"VARCHAR\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 5: \"TEXT2\"\n"
			+ "            TAG 0x19 UINT 6: \"type\"\n"
			+ "            TEXT 4: \"CLOB\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 5: \"DATE1\"\n"
			+ "            TAG 0x19 UINT 6: \"type\"\n"
			+ "            TEXT 4: \"DATE\"\n"
			+ "TAG 0x0102(UINT 10000, UINT 4096) IARRAY\n"
			+ "    ARRAY 5\n"
			+ "        UINT 1\n"
			+ "        IBYTES\n"
			+ "            BYTES 32: 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F\n"
			+ "        BREAK\n"
			+ "        TEXT 21: \"^ Starts with a caret\"\n"
			+ "        ITEXT\n"
			+ "            TEXT 32: \"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000B\\f\\r\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F\"\n"
			+ "        BREAK\n"
			+ "        TAG 0x01 UINT 1464472800\n"
			+ "    ARRAY 5\n"
			+ "        UINT 2\n"
			+ "        IBYTES\n"
			+ "            BYTES 256: 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A 58 58 58 58 58 58 58 58 58 58 58 58 58 58 58 0A\n"
			+ "        BREAK\n"
			+ "        TEXT 29: \"Does not start with a ^ caret\"\n"
			+ "        ITEXT\n"
			+ "            TEXT 256: \"XXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\nXXXXXXXXXXXXXXX\\n\"\n"
			+ "        BREAK\n"
			+ "        TAG 0x01 UINT 1465336800\n"
			+ "BREAK\n" );
	}

	@Test
	public void testExport2() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-export-cbor2.sql" );
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

		CBORToString toString = new CBORToString( Resources.getResource( "export21.cbor" ).newInputStream() );
		String s = toString.toString();
		System.out.println( s );
		assertThat( s ).isEqualTo( "MAP 3\n"
			+ "    TEXT 7: \"version\"\n"
			+ "    UINT 1\n"
			+ "    TEXT 11: \"description\"\n"
			+ "    TEXT 29: \"SolidBase CBOR Data Dump File\"\n"
			+ "    TEXT 9: \"createdBy\"\n"
			+ "    MAP 2\n"
			+ "        TEXT 7: \"product\"\n"
			+ "        TEXT 9: \"SolidBase\"\n"
			+ "        TEXT 7: \"version\"\n"
			+ "        TEXT 5: \"2.0.0\"\n"
			+ "TAG 0x0100 MAP 1\n"
			+ "    TEXT 6: \"fields\"\n"
			+ "    ARRAY 19\n"
			+ "        MAP 4\n"
			+ "            TEXT 10: \"schemaName\"\n"
			+ "            TEXT 6: \"PUBLIC\"\n"
			+ "            TEXT 9: \"tableName\"\n"
			+ "            TEXT 5: \"TEMP1\"\n"
			+ "            TEXT 4: \"name\"\n"
			+ "            TEXT 7: \"TINYINT\"\n"
			+ "            TEXT 4: \"type\"\n"
			+ "            TAG 0x19 UINT 6: \"TINYINT\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 8: \"SMALLINT\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 8: \"SMALLINT\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 7: \"INTEGER\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 9: \"INTEGER\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 6: \"BIGINT\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 10: \"BIGINT\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 7: \"DECIMAL\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 11: \"DECIMAL\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 5: \"FLOAT\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TEXT 6: \"DOUBLE\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 7: \"BOOLEAN\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 14: \"BOOLEAN\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"CHAR\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 15: \"CHAR\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 7: \"VARCHAR\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 16: \"VARCHAR\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"CLOB\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 17: \"CLOB\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 6: \"BINARY\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 18: \"BINARY\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 9: \"VARBINARY\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 19: \"VARBINARY\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"BLOB\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 20: \"BLOB\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"DATE\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 21: \"DATE\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 4: \"TIME\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 22: \"TIME\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 9: \"TIMESTAMP\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 23: \"TIMESTAMP\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 5: \"DATEZ\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 21: \"DATE\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 5: \"TIMEZ\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 22: \"TIME\"\n"
			+ "        MAP 4\n"
			+ "            TAG 0x19 UINT 1: \"schemaName\"\n"
			+ "            TAG 0x19 UINT 2: \"PUBLIC\"\n"
			+ "            TAG 0x19 UINT 3: \"tableName\"\n"
			+ "            TAG 0x19 UINT 4: \"TEMP1\"\n"
			+ "            TAG 0x19 UINT 5: \"name\"\n"
			+ "            TEXT 10: \"TIMESTAMPZ\"\n"
			+ "            TAG 0x19 UINT 7: \"type\"\n"
			+ "            TAG 0x19 UINT 23: \"TIMESTAMP\"\n"
			+ "TAG 0x0102(UINT 10000, UINT 4096) IARRAY\n"
			+ "    ARRAY 19\n"
			+ "        UINT 1\n"
			+ "        UINT 1\n"
			+ "        UINT 1\n"
			+ "        UINT 1\n"
			+ "        TEXT 1: \"1\"\n"
			+ "        DFLOAT 1.0\n"
			+ "        BOOL true\n"
			+ "        TEXT 1: \"Y\"\n"
			+ "        TEXT 4: \"test\"\n"
			+ "        ITEXT\n"
			+ "            TEXT 4: \"test\"\n"
			+ "        BREAK\n"
			+ "        BYTES 1: 01\n"
			+ "        BYTES 4: 01 02 03 04\n"
			+ "        IBYTES\n"
			+ "            BYTES 4: 01 02 03 04\n"
			+ "        BREAK\n"
			+ "        TAG 0x01 UINT 1464472800\n"
			+ "        TAG 0x01 UINT 60858\n"
			+ "        TAG 0x01 UINT 1464537258\n"
			+ "        TAG 0x01 UINT 1464472800\n"
			+ "        TAG 0x01 UINT 60858\n"
			+ "        TAG 0x01 UINT 1464537258\n"
			+ "BREAK\n"
		);

		processor.upgrade( "3" );

		statement = processor.prepareStatement( "SELECT * FROM TEMP1" );
		ResultSet result = statement.executeQuery();
		assertThat( result.next() ).isTrue();
		assertThat( result.next() ).isTrue();
		assertThat( result.next() ).isFalse();

		processor.end();
	}
}
