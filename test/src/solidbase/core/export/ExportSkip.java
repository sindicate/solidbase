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
import solidstack.io.Resources;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.json.JSONReader;

public class ExportSkip
{
	@Test
	public void testExportSkip() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "test-export-skip.sql" );
		try
		{
			processor.process();
		}
		finally
		{
			processor.end();
		}

		SourceReader reader = SourceReaders.forResource( Resources.getResource( "export-skip1.json" ), "UTF-8" );
		try
		{
			JSONReader json = new JSONReader( reader );
			JSONObject object = (JSONObject)json.read();
			Assert.assertNotNull( object );
			JSONArray array = object.getArray( "fields" );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getObject( 0 ).getString( "name" ), "FIELD1" );
			Assert.assertEquals( array.getObject( 1 ).getString( "name" ), "FIELD3" );
			Assert.assertEquals( array.getObject( 2 ).getString( "name" ), "FIELD5" );

			// [1,3,{"file":"export-skip1-4.txt","size":1}]
			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 1 ) );
			Assert.assertEquals( array.getNumber( 1 ), BigDecimal.valueOf( 3 ) );
			object = array.getObject( 2 );
			Assert.assertEquals( object.getString( "file" ), "export-skip1-4.txt" );
			Assert.assertEquals( object.getNumber( "size" ), BigDecimal.ONE );

			array = (JSONArray)json.read();
			Assert.assertNull( array );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-skip1-4.txt" ), "UTF-8" );
		try
		{
			Assert.assertEquals( reader.readLine(), "5" );
			Assert.assertNull( reader.readLine() );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-skip2.json" ), "UTF-8" );
		try
		{
			JSONReader json = new JSONReader( reader );
			JSONObject object = (JSONObject)json.read();
			Assert.assertNotNull( object );
			JSONArray array = object.getArray( "fields" );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getObject( 0 ).getString( "name" ), "FIELD1" );
			Assert.assertEquals( array.getObject( 1 ).getString( "name" ), "FIELD2" );
			Assert.assertEquals( array.getObject( 2 ).getString( "name" ), "FIELD3" );

			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 1 ) );
			object = array.getObject( 1 );
			Assert.assertEquals( object.getString( "file" ), "export-skip2-4.txt" );
			Assert.assertEquals( object.getNumber( "size" ), BigDecimal.ONE );
			Assert.assertEquals( array.getNumber( 2 ), BigDecimal.valueOf( 3 ) );

			array = (JSONArray)json.read();
			Assert.assertNull( array );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-skip2-4.txt" ), "UTF-8" );
		try
		{
			Assert.assertEquals( reader.readLine(), "2" );
			Assert.assertNull( reader.readLine() );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-skip3.json" ), "UTF-8" );
		try
		{
			JSONReader json = new JSONReader( reader );
			JSONObject object = (JSONObject)json.read();
			Assert.assertNotNull( object );
			JSONArray array = object.getArray( "fields" );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getObject( 0 ).getString( "name" ), "FIELD1" );
			Assert.assertEquals( array.getObject( 1 ).getString( "name" ), "FIELD3" );
			Assert.assertEquals( array.getObject( 2 ).getString( "name" ), "FIELD5" );

			array = (JSONArray)json.read();
			Assert.assertNotNull( array );
			Assert.assertEquals( array.size(), 3 );
			Assert.assertEquals( array.getNumber( 0 ), BigDecimal.valueOf( 1 ) );
			Assert.assertEquals( array.getNumber( 1 ), BigDecimal.valueOf( 3 ) );
			object = array.getObject( 2 );
			Assert.assertEquals( object.getString( "file" ), "export-skip3-null.txt" );
			Assert.assertEquals( object.getNumber( "size" ), BigDecimal.ONE );

			array = (JSONArray)json.read();
			Assert.assertNull( array );
		}
		finally
		{
			reader.close();
		}

		reader = SourceReaders.forResource( Resources.getResource( "export-skip3-null.txt" ), "UTF-8" );
		try
		{
			Assert.assertEquals( reader.readLine(), "5" );
			Assert.assertNull( reader.readLine() );
		}
		finally
		{
			reader.close();
		}
	}
}
