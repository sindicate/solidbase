package solidbase.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorator that does not pass through calls to the {@link #close()} method. This is used to protect the output stream obtained from the socket.
 * 
 * @author René M. de Bloois
 */
public class CloseBlockingOutputStream extends FilterOutputStream
{
	/**
	 * Constructor.
	 * 
	 * @param out The real {@link OutputStream}.
	 */
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
