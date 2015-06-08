package solidbase.core.export;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
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

public class ExportCoalesce
{
	@Test
	public void testExportCoalesce() throws SQLException, UnsupportedEncodingException, FileNotFoundException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "test-export-coalesce.sql" );
		processor.process();
		processor.end();

		SourceReader reader = SourceReaders.forResource( Resources.getResource( "export-coalesce1.csv" ), "UTF-8" );
		try
		{
			CSVReader csv = new CSVReader( reader, ',', false );

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
	}
}
