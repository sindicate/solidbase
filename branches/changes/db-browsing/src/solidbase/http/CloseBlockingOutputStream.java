package solidbase.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CloseBlockingOutputStream extends FilterOutputStream
{
	public CloseBlockingOutputStream( OutputStream out )
	{
		super( out );
	}

	@Override
	public void close() throws IOException
	{
		// Stop the close cascade.
	}
}
