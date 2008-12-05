package com.logicacmg.idt.commons.io;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;

import com.logicacmg.idt.commons.util.Assert;

/**
 * This class wraps an {@link InputStream}. It enables the reading of lines from the input stream and getting and setting of the position in the input stream.
 * 
 * @author René M. de Bloois
 * @since Mar 29, 2006
 */
public class LineInputStream extends InputStream
{
	protected InputStream input;
	protected URL url;

	protected byte[] buffer;
	protected int buffercount;
	protected int bufferpos;
	protected int pos;

	protected String encoding = "ISO-8859-1";
	protected int bom;

	/**
	 * Wrap a {@link FileInputStream}.
	 * 
	 * @param input The file input stream.
	 * @throws IOException
	 */
	public LineInputStream( FileInputStream input ) throws IOException
	{
		this.input = input;
		init();
	}

	/**
	 * Construct a LineInputStream around a url.
	 * 
	 * @param url The url.
	 * @throws IOException
	 */
	public LineInputStream( URL url ) throws IOException
	{
		this.url = url;
		this.input = url.openStream();
		init();
	}

	// BOMS:
	// 00 00 FE FF  UTF-32, big-endian
	// FF FE 00 00 	UTF-32, little-endian
	// FE FF 	    UTF-16, big-endian
	// FF FE 	    UTF-16, little-endian
	// EF BB BF 	UTF-8
	protected void init() throws IOException
	{
		this.buffer = new byte[ 8192 ];
		this.buffercount = this.bufferpos = this.pos = 0;

		fill();
		if( this.buffercount >= 2 )
		{
			if( this.buffer[ 0 ] == -2 && this.buffer[ 1 ] == -1 )
			{
				this.encoding = "UTF-16BE";
				this.bom = 2;
				throw new UnsupportedOperationException( "UTF-16 not supported yet" );
			}
			if( this.buffer[ 0 ] == -1 && this.buffer[ 1 ] == -2 )
			{
				this.encoding = "UTF-16LE";
				this.bom = 2;
				throw new UnsupportedOperationException( "UTF-16 not supported yet" );
			}
		}
		if( this.buffercount >= 3 )
		{
			if( this.buffer[ 0 ] == -17 && this.buffer[ 1 ] == -69 && this.buffer[ 2 ] == -65 )
			{
				this.encoding = "UTF-8";
				this.bom = 3;
				return;
			}
		}
	}

	public String getEncoding()
	{
		return this.encoding;
	}

	public void setEncoding( String encoding )
	{
		this.encoding = encoding;
	}

	/**
	 * Fills up the buffer.
	 * 
	 * @return true if bytes are read, false otherwise
	 * @throws IOException
	 */
	protected boolean fill() throws IOException
	{
		Assert.isTrue( this.bufferpos >= this.buffercount );

		this.buffercount = this.input.read( this.buffer, 0, 8192 );
		this.bufferpos = 0;
		return this.bufferpos < this.buffercount;
	}

	@Override
	public int read() throws IOException
	{
		if( this.bufferpos >= this.buffercount )
			if( !fill() )
				throw new EOFException();

		this.pos++;
		return this.buffer[ this.bufferpos++ ];
	}

	/**
	 * Concatenates two byte arrays.
	 * 
	 * @param b1
	 * @param b2
	 * @param start
	 * @param end
	 * @return the resulting byte array
	 */
	protected byte[] append( byte[] b1, byte[] b2, int start, int end )
	{
		int len = end - start;
		byte[] result;

		if( b1 != null )
		{
			result = new byte[ b1.length + len ];
			System.arraycopy( b1, 0, result, 0, b1.length );
			System.arraycopy( b2, start, result, b1.length, len );
		}
		else
		{
			result = new byte[ len ];
			System.arraycopy( b2, start, result, 0, len );
		}

		return result;
	}

	/**
	 * Reads a line of bytes. A line is ended with a \r, \n or \r\n.
	 * 
	 * @return null if end of file reached
	 * @throws IOException
	 */
	public byte[] readLineOfBytes() throws IOException
	{
		byte[] result = null;

		while( true )
		{
			if( this.bufferpos >= this.buffercount )
				if( !fill() )
					return result;

			int pos = this.bufferpos;
			byte b;
			do
				b = this.buffer[ pos++ ];
			while( b != '\n' && b != '\r' && pos < this.buffercount );

			if( b == '\n' || b == '\r' ) // \n or \r found
			{
				result = append( result, this.buffer, this.bufferpos, pos - 1 );

				this.pos += pos - this.bufferpos;
				this.bufferpos = pos;

				if( b == '\r' )
					if( this.bufferpos >= this.buffercount )
						if( fill() )
							if( this.buffer[ 0 ] == '\n' )
							{
								this.bufferpos++;
								this.pos++;
							}

				return result;
			}

			result = append( result, this.buffer, this.bufferpos, pos );
			this.pos += pos - this.bufferpos;
			this.bufferpos = pos;
		}
	}

	public String readLine() throws IOException
	{
		long pos = getPosition();
		byte[] bytes = readLineOfBytes();
		if( bytes == null )
			return null;
		if( pos < this.bom )
		{
			int skip = this.bom - (int)pos;
			return new String( bytes, skip, bytes.length - skip, this.encoding );
		}
		return new String( bytes, this.encoding );
	}

	@Override
	public int available()
	{
		return this.buffercount - this.bufferpos;
	}

	/**
	 * Returns the current position within the stream.
	 * 
	 * @return the current position within the stream
	 * @throws IOException
	 */
	public long getPosition() throws IOException
	{
		if( this.input instanceof FileInputStream )
		{
			FileChannel channel = ( (FileInputStream)this.input ).getChannel();
			return channel.position() - this.buffercount + this.bufferpos;
		}

		return this.pos;
	}

	/**
	 * Sets the current position. If the wrapped stream is a {@link FileInputStream} it will use the file channel to set
	 * the position in the file. Otherwise, it will skip forward to the requested position. To skip backwards it will first close and reopen the {@link InputStream} via the url given during
	 * construction.
	 * 
	 * @param pos
	 * @throws IOException
	 */
	public void setPosition( long pos ) throws IOException
	{
		if( this.input instanceof FileInputStream )
		{
			FileChannel channel = ( (FileInputStream)this.input ).getChannel();
			channel.position( pos );
			this.buffercount = this.bufferpos = 0;
		}
		else
		{
			// TODO (RBloois -> RBloois) Maybe backwards skipping is possible for the given stream. Should we just try and catch the exception?
			if( getPosition() > pos )
			{
				this.input.close();
				this.input = this.url.openStream();
				this.pos = 0;
				this.buffercount = this.bufferpos = 0;
			}
			long skip = pos - getPosition();
			if( skip > 0 )
			{
				long result = skip( skip );
				Assert.isTrue( result == skip, "Skipped " + result + " bytes instead of " + skip );
			}
		}
	}
}
