package solidbase.core;

import java.io.File;
import java.io.IOException;

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Delimiter.Type;
import solidbase.util.RandomAccessLineReader;

public class Delimiters
{
	@Test(groups="new")
	public void testDelimiterRegexpCharacter() throws IOException
	{
		Mockit.tearDownMocks();

		String contents = "COMMAND\n^\n";
		Mockit.redefineMethods( RandomAccessLineReader.class, new MockRandomAccessLineReader( contents ) );

		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "" ) );
		SQLFile file = new SQLFile( ralr );
		file.setDelimiters( new Delimiter[] { new Delimiter( "^", Type.ISOLATED ) } );

		Command command = file.readStatement();
		assert command != null;

		Assert.assertEquals( command.getCommand(), "COMMAND\n" );

		file.close();
	}
}
