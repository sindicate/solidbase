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

import solidbase.core.CommandFileException;


/**
 * This is a tokenizer for CSV. It maintains the current line number, and it ignores whitespace.
 *
 * @author René M. de Bloois
 */
public class JSONTokenizer
{
	/**
	 * The reader used to read from and push back characters.
	 */
	protected PushbackReader in;

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
	 */
	public JSONTokenizer( LineReader in )
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
			case '\n':
			case '\r':
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
		StringBuilder result = this.result;
		result.setLength( 0 );

		int ch = this.in.read();

		// Ignore whitespace
		while( isWhitespace( ch ) && ch != ',' )
			ch = this.in.read();

		if( ch == ',' )
			return Token.VALUE_SEPARATOR;
		if( ch == ':' )
			return Token.NAME_SEPARATOR;
		if( ch == '[' )
			return Token.BEGIN_ARRAY;
		if( ch == ']' )
			return Token.END_ARRAY;
		if( ch == '{' )
			return Token.BEGIN_OBJECT;
		if( ch == '}' )
			return Token.END_OBJECT;
		if( ch == -1 )
			return Token.EOI;

		// Read a string enclosed by "
		if( ch == '"' )
		{
			while( true )
			{
				ch = this.in.read();
				if( ch == -1 )
					throw new CommandFileException( "Missing \"", this.in.getLocation() );
				if( ch == '"' )
					break;
				result.append( (char)ch );
			}
			return new Token( Token.STRING, result.toString() );
		}

		// A number
		if( ch == '-' )
		{
			result.append( (char)ch );
			ch = this.in.read();
			if( !( ch >= '0' && ch <= '9' ) )
				throw new CommandFileException( "Invalid number", this.in.getLocation() );
		}

		if( ch >= '0' && ch <= '9' )
		{
			while( ch >= '0' && ch <= '9' )
			{
				result.append( (char)ch );
				ch = this.in.read();
			}
			if( ch == '.' )
			{
				result.append( (char)ch );
				ch = this.in.read();
				if( !( ch >= '0' && ch <= '9' ) )
					throw new CommandFileException( "Invalid number", this.in.getLocation() );
				while( ch >= '0' && ch <= '9' )
				{
					result.append( (char)ch );
					ch = this.in.read();
				}
			}
			if( ch == 'E' || ch == 'e' )
			{
				result.append( (char)ch );
				ch = this.in.read();
				if( ch == '-' )
				{
					result.append( (char)ch );
					ch = this.in.read();
				}
				if( !( ch >= '0' && ch <= '9' ) )
					throw new CommandFileException( "Invalid number", this.in.getLocation() );
				while( ch >= '0' && ch <= '9' )
				{
					result.append( (char)ch );
					ch = this.in.read();
				}
			}
			this.in.push( ch );
			return new Token( Token.NUMBER, result.toString() );
		}

		throw new CommandFileException( "Unexpected character '" + (char)ch + "'", this.in.getLocation() );
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
	 * Returns the current file location.
	 *
	 * @return The current file location.
	 */
	public FileLocation getLocation()
	{
		return this.in.getLocation();
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
	 * A CSV token.
	 *
	 * @author René M. de Bloois
	 */
	static public class Token
	{
		static final protected char STRING = '"';
		static final protected char NUMBER = '0';

		/** A end-of-input token */
		static final protected Token EOI = new Token( (char)0 );

		static final protected Token BEGIN_ARRAY = new Token( '[' );
		static final protected Token END_ARRAY = new Token( ']' );
		static final protected Token BEGIN_OBJECT = new Token( '{' );
		static final protected Token END_OBJECT = new Token( '}' );
		static final protected Token NAME_SEPARATOR = new Token( ':' );
		static final protected Token VALUE_SEPARATOR = new Token( ',' );

		/**
		 * The type of the token.
		 */
		private char type;

		/**
		 * The value of the token.
		 */
		private String value;

		/**
		 * Constructs a new token.
		 *
		 * @param type The type of the token.
		 */
		private Token( char type )
		{
			this.type = type;
		}

		/**
		 * Constructs a new token.
		 *
		 * @param value The value of the token.
		 */
		protected Token( String value )
		{
			this.value = value;
		}

		protected Token( char type, String value )
		{
			this.type = type;
			this.value = value;
		}

		/**
		 * Returns the value of token.
		 *
		 * @return The value of token.
		 */
		public String getValue()
		{
			if( this.value == null )
				throw new IllegalStateException( "Value is null" );
			return this.value;
		}

		/**
		 * Is this token the end-of-input token?
		 *
		 * @return True if this token is the end-of-input token, false otherwise.
		 */
		public boolean isEndOfInput()
		{
			return this.type == 0;
		}

		/**
		 * Is this token the end-of-input token?
		 *
		 * @return True if this token is the end-of-input token, false otherwise.
		 */
		public boolean isBeginArray()
		{
			return this.type == '[';
		}
		public boolean isEndArray()
		{
			return this.type == ']';
		}
		public boolean isBeginObject()
		{
			return this.type == '{';
		}
		public boolean isEndObject()
		{
			return this.type == '}';
		}
		public boolean isNameSeparator()
		{
			return this.type == ':';
		}
		public boolean isValueSeparator()
		{
			return this.type == ',';
		}
		public boolean isString()
		{
			return this.type == STRING;
		}
		public boolean isNumber()
		{
			return this.type == NUMBER;
		}

		@Override
		public String toString()
		{
			if( this.value != null )
				return this.value;
			if( this.type == -1 )
				return "End-of-input";
			return String.valueOf( this.type );
		}
	}
}
