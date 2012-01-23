/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.io;

import java.io.IOException;
import java.io.Reader;


/**
 * Wraps a {@link Reader} and adds a line counting functionality.
 *
 * @author René M. de Bloois
 */
public class ReaderLineReader implements LineReader
{
	/**
	 * The reader used to read from the string.
	 */
	protected Reader reader;

	/**
	 * The current line the reader is positioned on.
	 */
	protected int currentLineNumber = 1;

	/**
	 * Buffer to contain a character that has been read by mistake.
	 */
	protected int buffer = -1;

	/**
	 * Buffer to contain the line that is being read.
	 */
	protected StringBuilder line;

	/**
	 * The underlying resource.
	 */
	protected Resource resource;


	/**
	 * Constructor.
	 */
	protected ReaderLineReader()
	{
		// Used by sub classes
	}

	/**
	 * Constructor.
	 *
	 * @param reader The reader to read from.
	 */
	public ReaderLineReader( Reader reader )
	{
		this.reader = reader;
	}

	public void init( Reader reader )
	{
		this.reader = reader;
		this.currentLineNumber = 1;
		this.buffer = -1;
		if( this.line != null )
			this.line.setLength( 0 );
	}

	/**
	 * Close the reader and the underlying reader.
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
				throw new FatalIOException( e );
			}
			this.reader = null;
		}
	}

	public String readLine()
	{
		if( this.line == null )
			this.line = new StringBuilder();

		while( true )
		{
			int ch = read();
			switch( ch )
			{
				case -1:
					if( this.line.length() == 0 )
						return null;
					//$FALL-THROUGH$
				case '\n':
					String result = this.line.toString();
					this.line.setLength( 0 );
					return result;
				default:
					this.line.append( (char)ch );
			}
		}
	}

	public int getLineNumber()
	{
		if( this.reader == null )
			throw new IllegalStateException( "Closed" );
		return this.currentLineNumber;
	}

	public int read()
	{
		try
		{
			int result;
			if( this.buffer >= 0 )
			{
				result = this.buffer;
				this.buffer = -1;
			}
			else
				result = this.reader.read();

			switch( result )
			{
				case '\r':
					result = this.reader.read();
					if( result != '\n' )
						this.buffer = result;
					//$FALL-THROUGH$
				case '\n':
					this.currentLineNumber++;
					return '\n';
				default:
					return result;
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	public Resource getResource()
	{
		return this.resource;
	}

	public String getEncoding()
	{
		return "internal";
	}

	public byte[] getBOM()
	{
		return null;
	}

	public FileLocation getLocation()
	{
		return new FileLocation( this.resource, getLineNumber() );
	}
}
