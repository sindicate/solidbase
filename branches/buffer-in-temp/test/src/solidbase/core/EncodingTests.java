package solidbase.core;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidstack.io.MemoryResource;
import solidstack.io.RandomAccessSourceReader;
import solidstack.io.Resource;

public class EncodingTests
{
	@Test(groups="new")
	static public void test1() throws UnsupportedEncodingException, FileNotFoundException
	{
		String text = "\u00EF\u00BB\u00BF--* ENCODING \"UTF-8\"\n" +
				"--* DEFINITION\n" +
				"--* /DEFINITION";
		byte[] bytes = text.getBytes( "ISO-8859-1" );
		Assert.assertEquals( bytes[ 0 ], -17 );
		Assert.assertEquals( bytes[ 1 ], -69 );
		Assert.assertEquals( bytes[ 2 ], -65 );

		Resource resource = new MemoryResource( bytes );
		RandomAccessSourceReader reader = new RandomAccessSourceReader( resource, EncodingDetector.INSTANCE );
		UpgradeFile result = new UpgradeFile( reader );
		result.scan();
	}
}
