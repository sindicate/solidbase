package solidbase.core;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Delimiter.Type;

public class Delimiters
{
	@Test
	public void testDelimiterRegexpCharacter() throws IOException
	{
		String contents = "COMMAND\n^\n";
		SQLSource source = new SQLSource( contents );
		source.setDelimiters( new Delimiter[] { new Delimiter( "^", Type.ISOLATED ) } );
		Command command = source.readCommand();
		assert command != null;
		Assert.assertEquals( command.getCommand(), "COMMAND\n" );
	}
}
