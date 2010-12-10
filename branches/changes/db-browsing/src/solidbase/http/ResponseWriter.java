package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class ResponseWriter extends Writer
{
	protected OutputStreamWriter writer;
	protected String contentType;

	public ResponseWriter( OutputStream out, String charsetName, String contentType )
	{
		super( out /* = lock object */ );

		this.contentType = contentType;
		try
		{
			this.writer = new OutputStreamWriter( out, charsetName );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new HttpException( e );
		}
	}

	public ResponseWriter( OutputStream out, String charsetName )
	{
		this( out, charsetName, null );
	}

	@Override
	public void write( char[] cbuf, int off, int len )
	{
		try
		{
			this.writer.write( cbuf, off, len );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( int c )
	{
		try
		{
			this.writer.write( c );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( char[] cbuf )
	{
		try
		{
			this.writer.write( cbuf );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( String str )
	{
		try
		{
			this.writer.write( str );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( String str, int off, int len )
	{
		try
		{
			this.writer.write( str, off, len );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	public void writeEncoded( String str )
	{
		if( this.contentType != null && this.contentType.equals( "text/html" ) )
		{
			for( char ch : str.toCharArray() )
			{
				switch( ch )
				{
					case '<': write( "&lt;" ); break;
					case '>': write( "&gt;" ); break;
					case '&': write( "&amp;" ); break;
					default: write( ch );
				}
			}
		}
		else
			write( str );
	}

	public void writeEncoded( Object o )
	{
		if( o != null )
			writeEncoded( o.toString() );
	}

	public void write( Object o )
	{
		write( o != null ? o.toString() : "null" );
	}

	@Override
	public void flush()
	{
		try
		{
			this.writer.flush();
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
		// The reponse writer does not need to flush.
		// We don't want to trigger the flushing of the output stream.
	}

	@Override
	public void close()
	{
		throw new UnsupportedOperationException();
	}

	public String getEncoding()
	{
		return this.writer.getEncoding();
	}
}
