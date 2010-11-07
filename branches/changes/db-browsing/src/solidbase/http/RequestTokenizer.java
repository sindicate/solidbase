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

package solidbase.http;

import solidbase.core.CommandFileException;
import solidbase.util.LineReader;
import solidbase.util.PushbackReader;


public class RequestTokenizer
{
	/**
	 * The reader used to read from and push back characters.
	 */
	protected PushbackReader in;


	/**
	 * Constructs a new instance of the HttpHeaderTokenizer.
	 * 
	 * @param in The input.
	 */
	public RequestTokenizer( PushbackReader in )
	{
		this.in = in;
	}

	/**
	 * Is the given character a whitespace?
	 * 
	 * @param ch The character to check.
	 * @return True if the characters is whitespace, false otherwise.
	 */
	protected boolean isWhitespace( int ch )
	{
		switch( ch )
		{
			case ' ':
			case '\t':
				return true;
		}
		return false;
	}

	public Token get()
	{
		int ch = this.in.read();

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != -1 )
			ch = this.in.read();

		StringBuilder result = new StringBuilder();
		while( !isWhitespace( ch ) && ch != '\n' )
		{
			if( ch == -1 )
				throw new HttpException( "Unexpected end of request" );
			result.append( (char)ch );
			ch = this.in.read();
		}

		this.in.push( ch );

		return new Token( result.toString() );
	}

	/**
	 * Returns a newline token. Throws a {@link CommandFileException} if another token is found.
	 * 
	 * @return The newline token.
	 */
	public Token getNewline()
	{
		int ch = this.in.read();

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != -1 )
			ch = this.in.read();

		// Check newline
		if( ch == -1 )
			throw new CommandFileException( "Unexpected end of statement", this.in.getLineNumber() );
		if( ch != '\n' )
			throw new CommandFileException( "Expecting end of line, not [" + (char)ch + "]", this.in.getLineNumber() );

		// Return the result
		return new Token( String.valueOf( (char)ch ) );
	}

	/**
	 * Returns the current line number.
	 * 
	 * @return The current line number.
	 */
	public int getLineNumber()
	{
		return this.in.getLineNumber();
	}

	/**
	 * Returns the underlying reader. But only if the back buffer is empty, otherwise an IllegalStateException is thrown.
	 * 
	 * @return The underlying reader.
	 */
	public LineReader getReader()
	{
		return this.in.getReader();
	}
}
