package solidbase.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FlushBlockingOutputStream extends FilterOutputStream
{
	public FlushBlockingOutputStream( OutputStream out )
	{
		super( out );
	}

	@Override
	public void flush() throws IOException
	{
		// Stop the flush cascade.
	}

	@Override
	public void close() throws IOException
	{
		// Stop the close cascade.
	}
}
