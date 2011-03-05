package solidbase.util;

import java.io.InputStream;
import java.net.URL;

public class InputStreamResource implements Resource
{
	protected InputStream inputStream;

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

	public URL getURL()
	{
		throw new UnsupportedOperationException();
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
	}
}
