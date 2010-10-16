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

import java.io.Reader;

import solidbase.core.Assert;
import solidbase.core.CommandFileException;


/**
 * This is a tokenizer for CSV. It maintains the current line number, and it ignores whitespace.
 * 
 * @author René M. de Bloois
 */
public class CSVTokenizer
{
	/**
	 * The reader used to read from and push back characters.
	 */
	protected PushbackReader in;

	/**
	 * The CSV separator.
	 */
	protected int separator;


	/**
	 * Constructs a new instance of the Tokenizer.
	 * 
	 * @param in The input.
	 * @param lineNumber The current line number.
	 * @param separator The CSV separator.
	 */
	public CSVTokenizer( Reader in, int lineNumber, int separator )
	{
		this.in = new PushbackReader( in, lineNumber );
		this.separator = separator;
	}

	/**
	 * Is the given character a whitespace?
	 * 
	 * @param ch The character to check.
	 * @return True if the characters is whitespace, false otherwise.
	 */
	protected boolean isWhitespace( int ch )
	{
		if( ch == ' ' )
			return true;
		if( ch == this.separator )
			return false;
		if( ch == '\t' )
			return true;
		if( ch == '\f' )
			return true;
		return false;
	}

	/**
	 * Returns the next token from the input.
	 * 
	 * @return A token from the input. Null if there are no more tokens available.
	 */
	public Token get()
	{
		// Read whitespace
		int ch = this.in.read();
		while( ch != -1 && isWhitespace( ch ) )
			ch = this.in.read();

		// Read a string enclosed by ' or "
		if( ch == '"' )
		{
			StringBuilder result = new StringBuilder( 32 );
			while( true )
			{
				result.append( (char)ch );

				ch = this.in.read();
				if( ch == -1 )
					throw new CommandFileException( "Unexpected end of statement", this.in.getLineNumber() );
				if( ch == '"' )
				{
					result.append( (char)ch );
					ch = this.in.read();
					if( ch != '"' ) // Double "" do not end the string
					{
						this.in.push( ch );
						break;
					}
				}
			}
			return new Token( result.toString() );
		}

		if( ch == this.separator || ch == '\n' )
			return new Token( String.valueOf( (char)ch ) );

		if( ch == -1 )
			return new Token( null );

		// Collect all characters until separator or newline or EOI
		StringBuilder result = new StringBuilder( 16 );
		StringBuilder whiteSpace = new StringBuilder();
		do
		{
			if( isWhitespace( ch ) )
				whiteSpace.append( (char)ch );
			else
			{
				if( whiteSpace.length() > 0 )
				{
					result.append( whiteSpace );
					whiteSpace.setLength( 0 );
				}
				result.append( (char)ch );
			}
			ch = this.in.read();
		}
		while( ch != this.separator && ch != -1 && ch != '\n' );

		// Push back the last character
		this.in.push( ch );

		// Return the result
		Assert.isFalse( result.length() == 0 );
		return new Token( result.toString() );
	}

	/**
	 * A token that matches one of the expected tokens. Throws a {@link CommandFileException} if a token is encountered
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

		if( token.isEndOfInput() )
		{
			for( String exp : expected )
				if( exp == null )
					return token;
		}
		else
		{
			for( String exp : expected )
				if( token.equals( exp ) )
					return token;
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

		throw new CommandFileException( error.toString(), lineNumber );
	}

	/**
	 * Returns a newline token. Throws a {@link CommandFileException} if another token is found.
	 * 
	 * @return The newline token.
	 */
	public Token getNewline()
	{
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
			throw new CommandFileException( "Unexpected end of statement", this.in.getLineNumber() );
		if( ch != '\n' )
			throw new CommandFileException( "Expecting end of line, not [" + (char)ch + "]", this.in.getLineNumber() );

		// Return the result
		return new Token( String.valueOf( (char)ch ) );
	}

//	/**
//	 * Push back a token.
//	 *
//	 * @param token The token to push back.
//	 */
//	public void push( Token token )
//	{
//		this.in.push( token.getValue() );
//		this.in.push( token.getWhiteSpace() );
//	}

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
	public Reader getReader()
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

		/**
		 * Does a comparison with the given string.
		 *
		 * @param s A string to compare the value of this token with.
		 * @return True if the value of this token and the given string are equal, false otherwise.
		 */
		public boolean equals( String s )
		{
			if( this.value == null )
				return false;
			return this.value.equals( s );
		}

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
