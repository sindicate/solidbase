package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import solidbase.core.SystemException;

public class MemoryResource implements Resource
{
	protected List< byte[] > buffer = new LinkedList< byte[] >();

	public MemoryResource()
	{
		// Default constructor
	}

	public MemoryResource( byte[] bytes )
	{
		this.buffer.add( bytes );
	}

	public MemoryResource( InputStream input )
	{
		append( input );
	}

	public boolean supportsURL()
	{
		return false;
	}

	public URL getURL()
	{
		throw new UnsupportedOperationException();
	}

	public InputStream getInputStream()
	{
		return new ByteMatrixInputStream( this.buffer.toArray( new byte[ this.buffer.size() ][] ) );
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
	}

	public void append( InputStream in )
	{
		byte[] buffer = new byte[ 4096 ];
		int count;
		try
		{
			while( ( count = in.read( buffer ) ) > 0 )
				this.buffer.add( Arrays.copyOf( buffer, count ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
