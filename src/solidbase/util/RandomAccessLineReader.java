/*--
 * Copyright 2005 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import solidbase.core.SystemException;


/**
 * A line reader that automatically detects character encoding through the BOM and is able to reposition itself on a line.
 * 
 * @author René M. de Bloois
 */
public class RandomAccessLineReader implements LineReader
{
	/**
	 * Constant for the ISO-8859-1 character set.
	 */
	static final public String CHARSET_ISO = "ISO-8859-1";

	/**
	 * Constant for the UTF-8 character set.
	 */
	static final public String CHARSET_UTF8 = "UTF-8";

	/**
	 * Constant for the UTF-16BE character set.
	 */
	static final public String CHARSET_UTF16BE = "UTF-16BE";

	/**
	 * Constant for the UTF-16LE character set.
	 */
	static final public String CHARSET_UTF16LE = "UTF-16LE";

	/**
	 * Constant for the default character set, which is ISO-8859-1.
	 */
	static final public String CHARSET_DEFAULT = CHARSET_ISO;

	/**
	 * The URL to read from.
	 */
	protected URL url;

	/**
	 * The reader used to read from the URL.
	 */
	protected BufferedReader reader;

	/**
	 * The current line the reader is positioned on.
	 */
	protected int currentLineNumber;

	/**
	 * The encoding of the stream.
	 */
	protected String encoding = CHARSET_DEFAULT;

	/**
	 * The Byte Order Mark found at the beginning of the stream.
	 */
	protected byte[] bom;

	/**
	 * Creates a new line reader from the given URL.
	 * 
	 * @param url The URL to read from.
	 */
	public RandomAccessLineReader( URL url )
	{
		try
		{
			this.url = url;
			reOpen();

			this.reader.mark( 1000 ); // 1000 is smaller then the default buffer size of 8192, which is ok
			String line = this.reader.readLine();
			//System.out.println( "First line [" + line + "]" );
			this.reader.reset();
			if( line == null )
				return;

			detectEncoding( line );

			if( this.bom != null )
				reOpen();
		}
		catch( IOException e )
		{
			close();
			throw new SystemException( e );
		}
		catch( RuntimeException e )
		{
			close();
			throw e;
		}
	}

	/**
	 * Creates a new line reader from the given file.
	 * 
	 * @param file The file to read from.
	 * @throws IOException When a {@link IOException} occurs.
	 */
	public RandomAccessLineReader( File file ) throws IOException
	{
		this( file.toURI().toURL() );
	}

	/**
	 * Reopens itself to reset the position or change the character encoding.
	 */
	protected void reOpen()
	{
		close();

		try
		{
			InputStream is = this.url.openStream();
			if( this.bom != null )
				is.read( new byte[ this.bom.length ] ); // Skip some bytes
			this.reader = new BufferedReader( new InputStreamReader( is, this.encoding ) );
			this.currentLineNumber = 1;
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Detects the encoding of the stream by looking at the first 2, 3 or 4 bytes.
	 * 
	 * @param firstLine The first line read from the stream.
	 */
	protected void detectEncoding( String firstLine )
	{
		// BOMS:
		// 00 00 FE FF  UTF-32, big-endian
		// FF FE 00 00 	UTF-32, little-endian
		// FE FF 	    UTF-16, big-endian
		// FF FE 	    UTF-16, little-endian
		// EF BB BF 	UTF-8

		try
		{
			byte[] bytes = firstLine.getBytes( CHARSET_DEFAULT );
			if( bytes.length >= 2 )
			{
				if( bytes.length >= 3 && bytes[ 0 ] == -17 && bytes[ 1 ] == -69 && bytes[ 2 ] == -65 )
				{
					this.encoding = CHARSET_UTF8;
					this.bom = new byte[] { -17, -69, -65 };
					return;
				}
				if( bytes[ 0 ] == -2 && bytes[ 1 ] == -1 )
				{
					this.encoding = CHARSET_UTF16BE;
					this.bom = new byte[] { -2, -1 };
					return;
				}
				if( bytes[ 0 ] == -1 && bytes[ 1 ] == -2 )
				{
					this.encoding = CHARSET_UTF16LE;
					this.bom = new byte[] { -1, -2 };
					return;
				}
			}
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Reopen the stream to change the character decoding.
	 * 
	 * @param encoding the requested encoding.
	 */
	public void reOpen( String encoding )
	{
		this.encoding = encoding;
		reOpen();
	}

	/**
	 * Close the reader. Other streams used by this reader are also closed.
	 */
	public void close()
	{
		if( this.reader != null )
		{
			try
			{
				this.reader.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			this.reader = null;
		}
	}

	/**
	 * Reads a line from the stream. The line number count is incremented.
	 * 
	 * @return The line that is read or null of there are no more lines.
	 */
	public String readLine()
	{
		try
		{
			String result = this.reader.readLine();
			if( result != null )
				this.currentLineNumber++;
			return result;
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Returns the current line number. The current line number is the line that is about to be read.
	 * 
	 * @return The current line number.
	 */
	public int getLineNumber()
	{
		if( this.reader == null )
			throw new IllegalStateException( "Stream is not open" );
		return this.currentLineNumber;
	}

	/**
	 * Repositions the stream so that the given line number is the one that is to be read next. The underlying stream will be reopened if needed.
	 * 
	 * @param lineNumber the number of the line that will be read next.
	 */
	public void gotoLine( int lineNumber )
	{
		if( this.reader == null )
			throw new IllegalStateException( "Stream is not open" );
		if( lineNumber < 1 )
			throw new IllegalArgumentException( "lineNumber must be 1 or greater" );
		if( lineNumber < this.currentLineNumber )
			reOpen();
		while( lineNumber > this.currentLineNumber )
			if( readLine() == null )
				throw new IllegalArgumentException( "lineNumber " + lineNumber + " not found" );
	}

	/**
	 * Returns the current character encoding of the stream.
	 * 
	 * @return The current character encoding of the stream.
	 */
	public String getEncoding()
	{
		return this.encoding;
	}

	/**
	 * Returns the Byte Order Mark found in the stream.
	 * 
	 * @return The Byte Order Mark found. Will be null if no BOM was present.
	 */
	public byte[] getBOM()
	{
		return this.bom;
	}
}
