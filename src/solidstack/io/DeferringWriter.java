package solidstack.io;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import solidbase.core.SystemException;

/**
 * A writer that does not write to the given resource until a certain threshold is reached.
 * 
 * @author René de Bloois
 */
public class DeferringWriter extends Writer
{
	protected int threshold;
	protected Resource resource;
	protected String encoding;

	protected StringBuilder buffer;
	protected Writer writer;

	public DeferringWriter( int threshold, Resource resource, String encoding ) throws UnsupportedEncodingException
	{
		if( !Charset.isSupported( encoding ) )
			throw new UnsupportedEncodingException( encoding );

		this.threshold = threshold;
		this.resource = resource;
		this.encoding = encoding;

		if( threshold > 0 )
			this.buffer = new StringBuilder();
	}

	protected void initWriter()
	{
		try
		{
			this.writer = new OutputStreamWriter( this.resource.getOutputStream(), this.encoding );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e ); // This can't happen
		}
	}

	@Override
	public void write( int c ) throws IOException
	{
		if( this.buffer != null )
		{
			this.buffer.append( (char)c );
			if( this.buffer.length() > this.threshold )
			{
				initWriter();
				this.writer.write( this.buffer.toString() );
				this.buffer = null;
			}
		}
		else
		{
			if( this.writer == null )
				initWriter();
			this.writer.write( c );
		}
	}

	@Override
	public void write( char[] cbuf, int off, int len ) throws IOException
	{
		if( this.buffer != null )
			if( this.buffer.length() + len > this.threshold )
			{
				initWriter();
				this.writer.write( this.buffer.toString() );
				this.writer.write( cbuf, off, len );
				this.buffer = null;
			}
			else
				this.buffer.append( cbuf, off, len );
		else
		{
			if( this.writer == null )
				initWriter();
			this.writer.write( cbuf, off, len );
		}
	}

	public boolean isInMemory()
	{
		return this.buffer != null;
	}

	public String getData()
	{
		return this.buffer.toString();
	}

	@Override
	public void flush() throws IOException
	{
		if( this.writer != null )
			this.writer.flush();
	}

	@Override
	public void close() throws IOException
	{
		if( this.writer != null )
			this.writer.close();
	}
}
