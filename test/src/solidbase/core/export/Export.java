package solidbase.core.export;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

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

		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 ( ID, PICTURE, TEXT, TEXT2, DATE1 ) VALUES ( ?, ?, ?, ?, ? )" );

		byte[] blob = new byte[ 32 ];
		for( int i = 0; i < blob.length; i++ )
			blob[ i ] = (byte)i;
		String blobString = new String( blob, "ISO-8859-1" );
		statement.setInt( 1, 1 );
		statement.setBinaryStream( 2, new ByteArrayInputStream( blob ) );
		statement.setString( 3, "^ Starts with a caret" );
		statement.setString( 4, blobString );
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
