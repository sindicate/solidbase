package solidbase.util;

import java.io.InputStream;
import java.net.URL;

/**
 * An input stream resource.
 *
 * @author René M. de Bloois
 */
public class InputStreamResource implements Resource
{
	/**
	 * The input stream.
	 */
	protected InputStream inputStream;

	/**
	 * Constructor.
	 *
	 * @param inputStream The input stream.
	 */
	public InputStreamResource( InputStream inputStream )
	{
		if( inputStream == null )
			throw new IllegalArgumentException( "inputStream should not be null" );
		this.inputStream = inputStream;
	}

	public InputStream getInputStream()
	{
		if( this.inputStream == null )
			throw new IllegalStateException( "inputStream has been accessed earlier" );
		InputStream result = this.inputStream;
		this.inputStream = null;
		return result;
	}

	public boolean supportsURL()
	{
		return false;
	}

	public URL getURL()
	{
		throw new UnsupportedOperationException();
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
	}
}
