package solidbase.test.util;

import java.io.FileNotFoundException;

import org.testng.annotations.Test;

import solidbase.util.JSONReader;
import solidbase.util.JSONWriter;
import solidstack.io.FileResource;
import solidstack.io.Resource;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;

public class JSON
{
	@Test
	public void testJSON1() throws FileNotFoundException
	{
		Resource resource = new FileResource( "json/test1.json" );
		SourceReader reader = SourceReaders.forResource( resource );
		JSONReader json = new JSONReader( reader );
		Object object = json.read();
		json.close();

		resource = new FileResource( "json/output1.json" );
		JSONWriter writer = new JSONWriter( resource );
		writer.writeFormatted( object, 80 );
		writer.close();
	}

	@Test
	public void testJSON2() throws FileNotFoundException
	{
		Resource resource = new FileResource( "json/test2.json" );
		SourceReader reader = SourceReaders.forResource( resource );
		JSONReader json = new JSONReader( reader );
		Object object = json.read();

		resource = new FileResource( "json/output2.json" );
		JSONWriter writer = new JSONWriter( resource );
		writer.writeFormatted( object, 80 );
		writer.close();
	}
}
