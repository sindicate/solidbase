package com.logicacmg.idt.commons.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;


/**
 * A line reader that automatically detects character encoding through the BOM and is able to reposition itself on a line.
 * 
 * @author René M. de Bloois
 */
public class RandomAccessLineReader
{
	static final public String CHARSET_ISO = "ISO-8859-1";
	static final public String CHARSET_UTF8 = "UTF-8";
	static final public String CHARSET_UTF16BE = "UTF-16BE";
	static final public String CHARSET_UTF16LE = "UTF-16LE";
	static final public String CHARSET_DEFAULT = CHARSET_ISO;

	protected URL url;
	protected BufferedReader reader;
	protected int currentLineNumber;
	protected String encoding = CHARSET_DEFAULT;
	protected int bomSize;

	/**
	 * Creates a new line reader from the given url.
	 * 
	 * @param url a url.
	 * @throws IOException
	 */
	public RandomAccessLineReader( URL url ) throws IOException
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

		if( this.bomSize > 0 )
			reOpen();

		// TODO close the reader in case of exception in this constructor
	}

	/**
	 * Creates a new line reader from the given file.
	 * 
	 * @param file a file.
	 * @throws IOException
	 */
	public RandomAccessLineReader( File file ) throws IOException
	{
		this( file.toURI().toURL() );
	}

	/**
	 * Reopens itself to reset the position or change the character encoding.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	protected void reOpen() throws UnsupportedEncodingException, IOException
	{
		InputStream is = this.url.openStream();
		if( this.bomSize > 0 )
			is.read( new byte[ this.bomSize ] );
		this.reader = new BufferedReader( new InputStreamReader( is, this.encoding ) );
		this.currentLineNumber = 1;
	}

	/**
	 * Detects the encoding by looking at the first 2, 3 or 4 bytes.
	 * 
	 * @param firstLine
	 * @throws UnsupportedEncodingException
	 */
	protected void detectEncoding( String firstLine ) throws UnsupportedEncodingException
	{
		// BOMS:
		// 00 00 FE FF  UTF-32, big-endian
		// FF FE 00 00 	UTF-32, little-endian
		// FE FF 	    UTF-16, big-endian
		// FF FE 	    UTF-16, little-endian
		// EF BB BF 	UTF-8

		byte[] bytes = firstLine.getBytes( CHARSET_DEFAULT );
		if( bytes.length >= 2 )
		{
			if( bytes.length >= 3 && bytes[ 0 ] == -17 && bytes[ 1 ] == -69 && bytes[ 2 ] == -65 )
			{
				this.encoding = CHARSET_UTF8;
				this.bomSize = 3;
				return;
			}
			if( bytes[ 0 ] == -2 && bytes[ 1 ] == -1 )
			{
				this.encoding = CHARSET_UTF16BE;
				this.bomSize = 2;
				return;
			}
			if( bytes[ 0 ] == -1 && bytes[ 1 ] == -2 )
			{
				this.encoding = CHARSET_UTF16LE;
				this.bomSize = 2;
				return;
			}
		}
	}

	/**
	 * Reopen the stream to change the character decoding.
	 * 
	 * @param encoding the requested encoding.
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public void reOpen( String encoding ) throws UnsupportedEncodingException, IOException
	{
		this.encoding = encoding;
		reOpen();
	}

	/**
	 * Close the reader and the underlying stream.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if( this.reader != null )
			this.reader.close();
	}

	/**
	 * Reads a line and increments the current line number.
	 * 
	 * @return the line that is read or null of there are no more lines.
	 * @throws IOException
	 */
	public String readLine() throws IOException
	{
		String result = this.reader.readLine();
		if( result != null )
			this.currentLineNumber++;
		return result;
	}

	/**
	 * Returns the current line number. The current line number is the line that is about to be read.
	 * 
	 * @return the current line number.
	 */
	public int getLineNumber()
	{
		return this.currentLineNumber;
	}

	/**
	 * Repositions the stream so that the given line number is the one that is read next. The underlying stream may be reopened if needed.
	 * 
	 * @param lineNumber the line number that you want to read next.
	 * @throws IOException
	 */
	public void gotoLine( int lineNumber ) throws IOException
	{
		if( lineNumber < 1 )
			throw new IllegalArgumentException( "lineNumber must be 1 or greater" );
		if( lineNumber < this.currentLineNumber )
			reOpen();
		while( lineNumber > this.currentLineNumber )
			if( readLine() == null )
				throw new IllegalArgumentException( "lineNumber " + lineNumber + " not found" );
	}

	/**
	 * Returns the current character encoding.
	 * 
	 * @return the current character encoding.
	 */
	public String getEncoding()
	{
		return this.encoding;
	}
}
