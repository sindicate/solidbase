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
	 * If true, whitespace is ignored around the values, but not inside double quoted values.
	 */
	protected boolean ignoreWhiteSpace;

	/**
	 * Buffer for the result.
	 */
	protected StringBuilder result = new StringBuilder( 256 );

	/**
	 * Buffer for pending whitespace.
	 */
	protected StringBuilder whiteSpace = new StringBuilder( 16 );


	/**
	 * Constructs a new instance of the Tokenizer.
	 * 
	 * @param in The input.
	 * @param separator The CSV separator.
	 * @param ignoreWhiteSpace Ignore white space, except white space enclosed in double quotes.
	 */
	public CSVTokenizer( LineReader in, int separator, boolean ignoreWhiteSpace )
	{
		this.in = new PushbackReader( in );
		this.separator = separator;
		this.ignoreWhiteSpace = ignoreWhiteSpace;
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
			case '\f':
				return true;
		}
		return false;
	}

	/**
	 * Returns the next token from the input.
	 * 
	 * @return A token from the input. Null if there are no more tokens available.
	 */
	public Token get()
	{
		boolean ignoreWhiteSpace = this.ignoreWhiteSpace;
		StringBuilder result = this.result;
		result.setLength( 0 );

		int ch = this.in.read();

		// Ignore whitespace
		if( ignoreWhiteSpace )
			while( isWhitespace( ch ) && ch != this.separator )
				ch = this.in.read();

		// Read a string enclosed by "
		if( ch == '"' )
		{
			while( true )
			{
				ch = this.in.read();
				if( ch == -1 )
					throw new CommandFileException( "Missing \"", this.in.getLineNumber() );
				if( ch == '"' )
				{
					ch = this.in.read();
					if( ch != '"' )
					{
						this.in.push( ch );
						break;
					}
					// Double "" do not end the string
				}
				result.append( (char)ch );
			}
			return new Token( result.toString() );
		}

		if( ch == this.separator || ch == '\n' )
			return new Token( String.valueOf( (char)ch ) );

		if( ch == -1 )
			return new Token( null );

		// Collect all characters until separator or newline or EOI
		StringBuilder whiteSpace = this.whiteSpace;
		whiteSpace.setLength( 0 );
		do
		{
			if( ch == '"' )
				throw new CommandFileException( "Unexpected \"", this.in.getLineNumber() );
			if( ignoreWhiteSpace )
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
			}
			else
			{
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
			// Empty string is also expected because of "" tokens
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
			if( this.value == null )
				return false;
			if( this.value.length() != 1 )
				return false;
			return this.value.charAt( 0 ) == '\n';
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

		/**
		 * Does a comparison with the given character.
		 *
		 * @param c A character to compare the value of this token with.
		 * @return True if the value of this token and the given character are equal, false otherwise.
		 */
		public boolean equals( char c )
		{
			if( this.value == null )
				return false;
			if( this.value.length() != 1 )
				return false;
			return this.value.charAt( 0 ) == c;
		}

		@Override
		public String toString()
		{
			return this.value;
		}
	}
}
