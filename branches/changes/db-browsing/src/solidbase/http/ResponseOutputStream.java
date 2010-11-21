package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;

import solidbase.util.Assert;

public class ResponseOutputStream extends OutputStream
{
	protected OutputStream out;
	protected Response response;
	protected byte[] buffer = new byte[ 8192 ];
	protected int pos;

	public ResponseOutputStream()
	{

	}

	public ResponseOutputStream( Response response, OutputStream out )
	{
		this.response = response;
		this.out = out;
	}

	@Override
	public void write( byte[] b, int off, int len )
	{
		try
		{
			if( this.response.isCommitted() )
			{
//				System.out.write( b, off, len );
				this.out.write( b, off, len );
			}
			else if( this.buffer.length - this.pos < len )
			{
				this.response.writeHeader( this.out );
//				System.out.write( this.buffer, 0, this.pos );
				this.out.write( this.buffer, 0, this.pos );
//				System.out.write( b, off, len );
				this.out.write( b, off, len );
			}
			else
			{
				System.arraycopy( b, off, this.buffer, this.pos, len );
				this.pos += len;
			}
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( byte[] b )
	{
		try
		{
			if( this.response.isCommitted() )
			{
//				System.out.write( b );
				this.out.write( b );
			}
			else if( this.buffer.length - this.pos < b.length )
			{
				this.response.writeHeader( this.out );
//				System.out.write( this.buffer, 0, this.pos );
				this.out.write( this.buffer, 0, this.pos );
//				System.out.write( b );
				this.out.write( b );
			}
			else
			{
				System.arraycopy( b, 0, this.buffer, this.pos, b.length );
				this.pos += b.length;
			}
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( int b )
	{
		try
		{
			if( this.response.isCommitted() )
			{
//				System.out.write( b );
				this.out.write( b );
			}
			else if( this.buffer.length - this.pos < 1 )
			{
				this.response.writeHeader( this.out );
//				System.out.write( this.buffer, 0, this.pos );
				this.out.write( this.buffer, 0, this.pos );
//				System.out.write( b );
				this.out.write( b );
			}
			else
			{
				this.buffer[ this.pos ] = (byte)b;
				this.pos ++;
			}
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void close()
	{
		flush();
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void flush()
	{
		try
		{
			if( this.response.isCommitted() )
				this.out.flush();
			else
			{
				this.response.writeHeader( this.out );
//				System.out.write( this.buffer, 0, this.pos );
				// The outputstream may be changed at this point
				this.out.write( this.buffer, 0, this.pos );
				this.out.flush();
			}
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	public void clear()
	{
		Assert.isFalse( this.response.isCommitted() );
		this.pos = 0;
	}
}
