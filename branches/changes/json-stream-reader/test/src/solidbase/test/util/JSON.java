package solidbase.test.util;

import org.testng.annotations.Test;

import solidbase.util.BOMDetectingLineReader;
import solidbase.util.FileResource;
import solidbase.util.JSONReader;
import solidbase.util.JSONWriter;
import solidbase.util.LineReader;
import solidbase.util.Resource;

public class JSON
{
	@Test
	public void testJSON1()
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
	public void testJSON2()
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
