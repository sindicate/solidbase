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
import java.io.IOException;
import java.io.StringReader;
import solidbase.core.SystemException;


/**
 * A line reader that reads from a string.
 * 
 * @author René M. de Bloois
 */
public class StringLineReader implements LineReader
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
	 * Creates a new line reader for the given input stream.
	 * 
	 * @param text The text to read from.
	 */
	public StringLineReader( String text )
	{
		this.reader = new BufferedReader( new StringReader( text ) );
		this.currentLineNumber = 1;
	}

	/**
	 * Creates a new line reader for the given input stream.
	 * 
	 * @param text The text to read from.
	 * @param lineNumber The line number of the text fragment in the file.
	 */
	public StringLineReader( String text, int lineNumber )
	{
		this.reader = new BufferedReader( new StringReader( text ) );
		this.currentLineNumber = lineNumber;
	}

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
}
