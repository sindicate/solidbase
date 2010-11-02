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

package solidbase.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

import solidbase.core.SystemException;


/**
 * Wraps a {@link BufferedReader} and adds a line counting functionality.
 * 
 * @author René M. de Bloois
 */
public class LineReader
{
	/**
	 * The reader used to read from the string.
	 */
	protected BufferedReader reader;

	/**
	 * The current line the reader is positioned on.
	 */
	protected int currentLineNumber;

	/**
	 * A line in the buffer, needed when characters are read with {@link #read()}.
	 */
	protected String buffer;

	/**
	 * The current position in the {@link #buffer}.
	 */
	protected int pos;

	protected URL url;


	/**
	 * Close the reader and the underlying input stream.
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
		if( this.buffer != null )
			throw new IllegalStateException( "There is a line in the buffer" );
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
			throw new IllegalStateException( "Closed" );
		return this.currentLineNumber;
	}

	/**
	 * Reads a character. Must always be repeated until a \n is encountered, otherwise {@link #readLine()} will fail. \r is never returned.
	 * 
	 * @return a character. \r is never returned.
	 */
	public int read()
	{
		if( this.buffer == null )
		{
			try
			{
				this.buffer = this.reader.readLine();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			if( this.buffer == null )
				return -1;
			this.pos = 0;
		}

		if( this.pos < this.buffer.length() )
		{
			int result = this.buffer.charAt( this.pos );
			this.pos++;
			return result;
		}

		this.buffer = null;
		this.currentLineNumber++;
		return '\n';
	}

	public URL getURL()
	{
		Assert.notNull( this.url );
		return this.url;
	}
}
