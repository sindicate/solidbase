package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import solidbase.core.SystemException;

public class ResponseWriter extends Writer
{
	protected OutputStreamWriter writer;

	public ResponseWriter( OutputStream out, String charsetName )
	{
		super( out /* = lock object */ );

		try
		{
			this.writer = new OutputStreamWriter( out, charsetName );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
		}
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
			throw new SystemException( e );
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
