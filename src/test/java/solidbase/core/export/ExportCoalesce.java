package solidbase.core.export;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.SQLProcessor;
import solidbase.core.Setup;
import solidbase.core.TestUtil;
import solidbase.util.CSVReader;
import solidstack.io.Resources;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.json.JSONReader;

public class ExportCoalesce
{
	@Test
	public void testExportCoalesce() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "test-export-coalesce.sql" );
		try
		{
			processor.process();
		}
		finally
		{
			processor.end();
		}

		SourceReader reader = SourceReaders.forResource( Resources.getResource( "export-coalesce1.csv" ), "UTF-8" );
		try
		{
			CSVReader csv = new CSVReader( reader, ',', true, false );

			String[] line = csv.getLine();
			Assert.assertEquals( line.length, 3 );
			Assert.assertEquals( line[ 0 ], "FIELD1" );
			Assert.assertEquals( line[ 1 ], "FIELD2" );
			Assert.assertEquals( line[ 2 ], "FIELD4" );

			line = csv.getLine();
			Assert.assertEquals( line.length, 3 );
			Assert.assertEquals( line[ 0 ], "1" );
			Assert.assertEquals( line[ 1 ], "2" );
			Assert.assertEquals( line[ 2 ], "4" );

			line = csv.getLine();
			Assert.assertEquals( line.length, 3 );
			Assert.assertEquals( line[ 0 ], "12" );
			Assert.assertEquals( line[ 1 ], "12" );
			Assert.assertEquals( line[ 2 ], "14" );

			line = csv.getLine();
			Assert.assertEquals( line.length, 3 );
			Assert.assertEquals( line[ 0 ], "23" );
			Assert.assertEquals( line[ 1 ], "25" );
			Assert.assertEquals( line[ 2 ], "24" );

			line = csv.getLine();
			Assert.assertNull( line );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-coalesce1.json" ), "UTF-8" );
		try
		{
			JSONReader json = new JSONReader( reader );
			JSONObject object = (JSONObject)json.read();
			Assert.assertNotNull( object );
			JSONArray array = object.getArray( "fields" );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getObject( 0 ).getString( "name" ), "FIELD1" );
			Assert.assertEquals( array.getObject( 1 ).getString( "name" ), "FIELD2" );
			Assert.assertEquals( array.getObject( 2 ).getString( "name" ), "FIELD4" );

			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 1 ) );
			Assert.assertEquals( array.getNumber( 1 ), BigDecimal.valueOf( 2 ) );
			Assert.assertEquals( array.getNumber( 2 ), BigDecimal.valueOf( 4 ) );

			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 12 ) );
			Assert.assertEquals( array.getNumber( 1 ), BigDecimal.valueOf( 12 ) );
			Assert.assertEquals( array.getNumber( 2 ), BigDecimal.valueOf( 14 ) );

			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 23 ) );
			Assert.assertEquals( array.getNumber( 1 ), BigDecimal.valueOf( 25 ) );
			Assert.assertEquals( array.getNumber( 2 ), BigDecimal.valueOf( 24 ) );

			array = (JSONArray)json.read();
			Assert.assertNull( array );
		}
		finally
		{
			reader.close();
		}
	}
}
