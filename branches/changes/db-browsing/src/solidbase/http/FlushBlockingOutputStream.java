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

	// This one has bad implementation in FilterOutputStream
	@Override
	public void write( byte[] b ) throws IOException
	{
		this.out.write( b );
	}

	// This one has bad implementation in FilterOutputStream
	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		this.out.write( b, off, len );
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
