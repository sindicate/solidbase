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

import solidbase.core.SystemException;
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
	public HttpHeaderTokenizer( LineReader in )
	{
		this.in = new PushbackReader( in );
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
				throw new SystemException( "Unexpected end of statement" );
			if( ch == '\n' )
				throw new SystemException( "Unexpected end of line" );
			result.append( (char)ch );
			ch = this.in.read();
		}

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != -1 )
			ch = this.in.read();

		if( ch != ':' )
			throw new SystemException( "Expecting a :" );

		// Return the result
		if( result.length() == 0 )
			throw new SystemException( "Empty header field" );

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
				throw new SystemException( "Unexpected end-of-input" );
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

	/**
	 * A token. The token is case insensitive, so the {@link #equals(String)} does a case insensitive comparison.
	 * 
	 * @author René M. de Bloois
	 */
	static public class Token
	{
		/**
		 * The value of the token.
		 */
		protected String value;

		/**
		 * Constructs a new token.
		 * 
		 * @param value The value of the token.
		 */
		public Token( String value )
		{
			this.value = value;
//			System.out.println( "Token [" + value + "]" );
		}

		/**
		 * Returns the value of token.
		 * 
		 * @return The value of token.
		 */
		public String getValue()
		{
			return this.value;
		}

		/**
		 * Is this token a newline?
		 * 
		 * @return True if this token is a newline, false otherwise.
		 */
		public boolean isNewline()
		{
			return this.value.charAt( 0 ) == '\n'; // Assume that if char 0 is a newline then the whole string is just the newline
		}

		/**
		 * Is this token the end-of-input token?
		 * 
		 * @return True if this token is the end-of-input token, false otherwise.
		 */
		public boolean isEndOfInput()
		{
			return this.value == null;
		}

//		/**
//		 * Does a case insensitive comparison with the given string.
//		 *
//		 * @param s A string to compare the value of this token with.
//		 * @return True if the value of this token and the given string are equal (ignoring case), false otherwise.
//		 */
//		public boolean equals( String s )
//		{
//			if( this.value == null )
//				return false;
//			return this.value.equalsIgnoreCase( s );
//		}

//		/**
//		 * The length of the value of this token.
//		 *
//		 * @return Length of the value of this token.
//		 */
//		public int length()
//		{
//			return this.value.length();
//		}

		@Override
		public String toString()
		{
			return this.value;
		}
	}
}
