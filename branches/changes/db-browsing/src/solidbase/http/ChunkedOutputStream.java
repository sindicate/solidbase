package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;

public class ChunkedOutputStream extends OutputStream
{
	protected OutputStream out;

	public ChunkedOutputStream( OutputStream out )
	{
		this.out = out;
	}

	@Override
	public void write( int b ) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void write( byte[] b ) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		if( len == 0 )
			return;
		this.out.write( Integer.toHexString( len ).toUpperCase().getBytes() ); // TODO Give a CharSet here?
		this.out.write( '\r' );
		this.out.write( '\n' );
		this.out.write( b, off, len );
		this.out.write( '\r' );
		this.out.write( '\n' );
	}

	@Override
	public void flush() throws IOException
	{
		this.out.flush();
	}

	@Override
	public void close() throws IOException
	{
		this.out.write( '0' );
		this.out.write( '\r' );
		this.out.write( '\n' );
		this.out.write( '\r' );
		this.out.write( '\n' );
		this.out.flush();
		this.out.close();
	}
}
