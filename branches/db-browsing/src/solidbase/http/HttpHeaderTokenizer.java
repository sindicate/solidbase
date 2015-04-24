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

import solidbase.util.LineReader;
import solidbase.util.PushbackReader;


public class HttpHeaderTokenizer
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
	public HttpHeaderTokenizer( PushbackReader in )
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

	public Token getField()
	{
		int ch = this.in.read();

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != -1 )
			ch = this.in.read();

		// Empty line
		if( ch == '\n' )
			return new Token( null );

		StringBuilder result = new StringBuilder();
		while( ch != ':' && !isWhitespace( ch ) )
		{
			if( ch == -1 )
				throw new HttpException( "Unexpected end of statement" );
			if( ch == '\n' )
				throw new HttpException( "Unexpected end of line" );
			result.append( (char)ch );
			ch = this.in.read();
		}

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != -1 )
			ch = this.in.read();

		if( ch != ':' )
			throw new HttpException( "Expecting a :" );

		// Return the result
		if( result.length() == 0 )
			throw new HttpException( "Empty header field" );

		return new Token( result.toString() );
	}

	public Token getValue()
	{
		// Read whitespace
		int ch = this.in.read();
		while( isWhitespace( ch ) )
			ch = this.in.read();

		// Read everything until end-of-line
		StringBuilder result = new StringBuilder();
		while( true )
		{
			if( ch == -1 )
				throw new HttpException( "Unexpected end-of-input" );
			if( ch == '\n' )
			{
				ch = this.in.read();
				if( ch != ' ' && ch != '\t' )
				{
					this.in.push( ch );
					return new Token( result.toString() );
				}
			}
			result.append( (char)ch );
			ch = this.in.read();
		}
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
