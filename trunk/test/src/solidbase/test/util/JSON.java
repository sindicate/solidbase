package solidbase.test.util;

import java.io.FileNotFoundException;

import org.testng.annotations.Test;

import solidbase.io.BOMDetectingLineReader;
import solidbase.io.FileResource;
import solidbase.io.LineReader;
import solidbase.io.Resource;
import solidbase.util.JSONReader;
import solidbase.util.JSONWriter;

public class JSON
{
	@Test
	public void testJSON1() throws FileNotFoundException
	{
		Resource resource = new FileResource( "json/test1.json" );
		LineReader reader = new BOMDetectingLineReader( resource );
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
		LineReader reader = new BOMDetectingLineReader( resource );
		JSONReader json = new JSONReader( reader );
		Object object = json.read();

		resource = new FileResource( "json/output2.json" );
		JSONWriter writer = new JSONWriter( resource );
		writer.writeFormatted( object, 80 );
		writer.close();
	}
}
