package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseOutputStream extends OutputStream
{
	protected OutputStream out;
	protected Response response;
	protected byte[] buffer = new byte[ 8192 ];
	protected int pos;

	public ResponseOutputStream( Response response, OutputStream out )
	{
		this.response = response;
		this.out = out;
	}

	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		if( this.response.isCommitted() )
			this.out.write( b, off, len );
		else if( this.buffer.length - this.pos < len )
		{
			this.response.writeHeader( this.out );
			this.out.write( this.buffer, 0, this.pos );
			this.out.write( b, off, len );
		}
		else
		{
			System.arraycopy( b, off, this.buffer, this.pos, len );
			this.pos += len;
		}
	}

	@Override
	public void write( byte[] b ) throws IOException
	{
		if( this.response.isCommitted() )
			this.out.write( b );
		else if( this.buffer.length - this.pos < b.length )
		{
			this.response.writeHeader( this.out );
			this.out.write( this.buffer, 0, this.pos );
			this.out.write( b );
		}
		else
		{
			System.arraycopy( b, 0, this.buffer, this.pos, b.length );
			this.pos += b.length;
		}
	}

	@Override
	public void write( int b ) throws IOException
	{
		if( this.response.isCommitted() )
			this.out.write( b );
		else if( this.buffer.length - this.pos < 1 )
		{
			this.response.writeHeader( this.out );
			this.out.write( this.buffer, 0, this.pos );
			this.out.write( b );
		}
		else
		{
			this.buffer[ this.pos ] = (byte)b;
			this.pos ++;
		}
	}

	@Override
	public void close() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() throws IOException
	{
		if( this.response.isCommitted() )
			this.out.flush();
		else
		{
			this.response.writeHeader( this.out );
			this.out.write( this.buffer, 0, this.pos );
			this.out.flush();
		}
	}
}
