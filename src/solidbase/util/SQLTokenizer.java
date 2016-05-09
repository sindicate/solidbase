/*--
 * Copyright 2010 Ren� M. de Bloois
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

import solidbase.core.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;


/**
 * This is a tokenizer for a language like SQL. It maintains the current line number, it is case insensitive, and it ignores whitespace.
 *
 * @author Ren� M. de Bloois
 */
// TODO Improve. See example JSPLikeTemplateParser.
public class SQLTokenizer
{
	/**
	 * The reader used to read from and push back characters.
	 */
	protected SourceReader in;

	private Token buffer;


	/**
	 * Constructs a new instance of the Tokenizer.
	 *
	 * @param in The input.
	 */
	public SQLTokenizer( SourceReader in )
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
			case '\n':
			case '\t':
			case '\f':
				return true;
		}
		return false;
	}

	/**
	 * Is the given character a special character?
	 *
	 * @param ch The character to check.
	 * @return True if the characters is a special character, false otherwise.
	 */
	static protected boolean isSpecial( int ch )
	{
		switch( ch )
		{
			case '!':
			case '"':
			case '#':
			case '%':
			case '&':
			case '\'':
			case '(':
			case ')':
			case '*':
			case '+':
			case ',':
			case '-':
			case '.':
			case '/':
			case ':':
			case ';':
			case '<':
			case '=':
			case '>':
			case '?':
			case '@':
			case '[':
			case '\\':
			case ']':
			case '^':
			case '`':
			case '{':
			case '|':
			case '}':
			case '~':
			case '\n':
			case '\t':
				return true;
		}
		return false;
	}

	/**
	 * Returns the next token from the input. The preceding whitespace is also contained separately in {@link Token#getWhiteSpace()}.
	 *
	 * @return A token from the input. Null if there are no more tokens available.
	 */
	public Token get()
	{
		if( this.buffer != null )
		{
			Token result = this.buffer;
			this.buffer = null;
			return result;
		}

		// Read whitespace
		StringBuilder whiteSpace = new StringBuilder();
		int ch = this.in.read();
		while( ch != -1 && isWhitespace( ch ) )
		{
			whiteSpace.append( (char)ch );
			ch = this.in.read();
		}

		// Read a string enclosed by ' or "
		if( ch == '\'' || ch == '"' )
		{
			StringBuilder result = new StringBuilder( 32 );
			int quote = ch;
			while( true )
			{
				result.append( (char)ch );

				ch = this.in.read();
				if( ch == -1 )
					throw new SourceException( "Unexpected end of statement", getLocation() );
				if( ch == quote )
				{
					result.append( (char)ch );
					ch = this.in.read();
					if( ch != quote ) // Double '' or "" do not end the string
					{
						this.in.rewind();
						break;
					}
				}
			}
			return new Token( result.toString(), whiteSpace.toString() );
		}

		if( isSpecial( ch ) )
			return new Token( String.valueOf( (char)ch ), whiteSpace.toString() );

		if( ch == -1 )
			return new Token( null, whiteSpace.toString() );

		// TODO What about the $ dollar sign?
		// Collect all characters until whitespace or special character
		StringBuilder result = new StringBuilder( 16 );
		do
		{
			result.append( (char)ch );
			ch = this.in.read();
		}
		while( ch != -1 && !isWhitespace( ch ) && !isSpecial( ch ) );

		// Push back the last character
		this.in.rewind();

		// Return the result
		Assert.isFalse( result.length() == 0 );
		return new Token( result.toString(), whiteSpace.toString() );
	}

	/**
	 * A token that matches one of the expected tokens. Throws a {@link SourceException} if a token is encountered
	 * that does not match the given expected tokens.
	 *
	 * @param expected The expected tokens.
	 * @return One of the expected tokens.
	 */
	public Token get( String... expected )
	{
		if( expected.length == 0 )
			throw new IllegalArgumentException( "Specify one ore more expected tokens" );

		Token token = get();
		expect( token, expected );
		return token;
	}

	/**
	 * Checks if the given token matches the expected tokens.
	 *
	 * @param token The token.
	 * @param expected The expected tokens.
	 */
	public void expect( Token token, String... expected )
	{
		if( expected.length == 0 )
			throw new IllegalArgumentException( "Specify one or more expected tokens" );

		if( token.isEndOfInput() )
		{
			for( String exp : expected )
				if( exp == null )
					return;
		}
		else
		{
			for( String exp : expected )
				if( token.eq( exp ) )
					return;
		}

		// Raise exception

		StringBuilder error = new StringBuilder();

		if( expected.length == 1 )
		{
			error.append( "Expecting [" );
			error.append( expected[ 0 ] != null ? expected[ 0 ] : "<end of statement>" );
			error.append( "]" );
		}
		else
		{
			error.append( "Expecting one of" );
			for( String exp : expected )
			{
				error.append( " [" );
				error.append( exp != null ? exp : "<end of statement>" );
				error.append( ']' );
			}
		}

		if( token.isEndOfInput() )
			error.append( ", not end-of-statement" );
		else
		{
			error.append( ", not [" );
			error.append( token );
			error.append( "]" );
		}

		int lineNumber = this.in.getLineNumber();
		if( token.isNewline() )
			lineNumber--;

		throw new SourceException( error.toString(), getLocation().lineNumber( lineNumber ) );
	}

	/**
	 * Returns a newline token. Throws a {@link SourceException} if another token is found.
	 *
	 * @return The newline token.
	 */
	public Token getNewline()
	{
		if( this.buffer != null )
		{
			Token result = this.buffer;
			this.buffer = null;
			if( !result.isNewline() )
				throw new SourceException( "Expecting end of line, not [" + result.getValue() + "]", getLocation() );
			return result;
		}

		// Read whitespace
		StringBuilder whiteSpace = new StringBuilder();
		int ch = this.in.read();
		while( ch != -1 && ch != '\n' && isWhitespace( ch ) )
		{
			whiteSpace.append( (char)ch );
			ch = this.in.read();
		}

		// Check newline
		if( ch == -1 )
			throw new SourceException( "Unexpected end of statement", getLocation() );
		if( ch != '\n' )
			throw new SourceException( "Expecting end of line, not [" + (char)ch + "]", getLocation() );

		// Return the result
		return new Token( String.valueOf( (char)ch ), whiteSpace.toString() );
	}

	/**
	 * Returns the remaining characters from the reader.
	 *
	 * @return the remaining characters from the reader.
	 */
	public String getRemaining()
	{
		StringBuilder result = new StringBuilder();
		for( int ch = this.in.read(); ch != -1; ch = this.in.read() )
			result.append( (char)ch );
		return result.toString();
	}

	/**
	 * Push back a token.
	 *
	 * @param token The token to push back.
	 */
	public void push( Token token )
	{
		if( this.buffer != null )
			throw new IllegalStateException( "Pushed token back twice" );
		this.buffer = token;
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
	public SourceReader getReader()
	{
		return this.in;
	}

	/**
	 * @return The location of this token.
	 */
	public SourceLocation getLocation()
	{
		return this.in.getLocation();
	}


	/**
	 * A token. Tokens are stored upper case, except for strings.
	 *
	 * @author Ren� M. de Bloois
	 */
	static public class Token
	{
		/**
		 * The value of the token.
		 */
		protected String value;

		/**
		 * The whitespace encountered before the token.
		 */
		protected String whiteSpace;

		/**
		 * Constructs a new token.
		 *
		 * @param value The value of the token.
		 * @param whiteSpace The whitespace encountered before the token.
		 */
		public Token( String value, String whiteSpace )
		{
			this.value = value != null && !value.startsWith( "\"" ) ? value.toUpperCase() : value;
			this.whiteSpace = whiteSpace;
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
		 * Returns the whitespace encountered before the token.
		 *
		 * @return The whitespace encountered before the token.
		 */
		public String getWhiteSpace()
		{
			return this.whiteSpace;
		}

		/**
		 * Is this token a newline?
		 *
		 * @return True if this token is a newline, false otherwise.
		 */
		public boolean isNewline()
		{
			if( this.value == null )
				return false;
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

		/**
		 * @return True if the token is a string.
		 */
		public boolean isString()
		{
			if( this.value == null )
				return false;
			return this.value.startsWith( "\"" );
		}

		/**
		 * @return The value but without the first and last character.
		 */
		public String stripQuotes()
		{
			return this.value.substring( 1, this.value.length() - 1 );
		}

		/**
		 * @return True if the token is a number.
		 */
		public boolean isNumber()
		{
			char ch = this.value.charAt( 0 );
			return ch >= '0' && ch <= '9';
		}

		/**
		 * Does a case sensitive comparison with the given string. Tokens are always upper case.
		 *
		 * @param s A string to compare the value of this token with.
		 * @return True if the value of this token and the given string are equal, false otherwise.
		 */
		public boolean eq( String s ) // TODO Compare with null?
		{
			if( this.value == null )
				return false;
			return this.value.equalsIgnoreCase( s );
		}

		/**
		 * The length of the value of this token.
		 *
		 * @return Length of the value of this token.
		 */
		public int length()
		{
			return this.value.length();
		}

		@Override
		public String toString()
		{
			if( this.value == null )
				return "EOF";
			if( this.value.charAt( 0 ) == '\n' ) // Assume that if char 0 is a newline then the whole string is just the newline
				return "NEWLINE";
			return this.value;
		}
	}
}
