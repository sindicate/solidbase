package solidbase.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NonClosingInputStream extends FilterInputStream
{
	public NonClosingInputStream( InputStream in )
	{
		super( in );
	}

	@Override
	public void close() throws IOException
	{
		// Stop the close cascade.
	}
}
