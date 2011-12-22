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
import solidbase.util.JSONTokenizer.Token;


/**
 * Reads JSON data from the given {@link LineReader}.
 *
 * @author René M. de Bloois
 */
public class JSONReader
{
	/**
	 * The source of tokens.
	 */
	protected JSONTokenizer tokenizer;

	/**
	 * The separator that separates the values.
	 */
	protected char separator;


	/**
	 * Constructor.
	 *
	 * @param reader The source of the JSON data.
	 */
	public JSONReader( LineReader reader )
	{
		this.tokenizer = new JSONTokenizer( reader );
	}

	/**
	 * Gets a line of values from the JSON data.
	 *
	 * @return A line of values from the JSON data.
	 */
	public Object read()
	{
		Token token = this.tokenizer.get();
//		System.out.println( "Token: " + token );
		// TODO Use switch(), need an enum for this
		if( token.isBeginObject() )
			return readObject();
		if( token.isBeginArray() )
			return readArray();
		if( token.isString() )
			return token.getValue();
		if( token.isNumber() )
			return token.getValue();
		if( token.isBoolean() )
			return token.getValue();
		if( token.isNull() )
			return token.getValue();
		if( token.isEndOfInput() )
			throw new JSONEndOfInputException();

		throw new CommandFileException( "Expecting {, [, \", a number, true, false or null, not '" + token + "'", this.tokenizer.getLocation() );
	}

	public JSONObject readObject()
	{
		JSONObject result = new JSONObject();

		Token token;
		do
		{
			token = this.tokenizer.get();
//			System.out.println( "Token: " + token );
			if( !token.isString() )
				throw new CommandFileException( "Expecting a name enclosed with \", not '" + token + "'", this.tokenizer.getLocation() );
			String name = (String)token.getValue();

			token = this.tokenizer.get();
//			System.out.println( "Token: " + token );
			if( !token.isNameSeparator() )
				throw new CommandFileException( "Expecting :, not '" + token + "'", this.tokenizer.getLocation() );

			Object value = read();
			result.set( name, value );

			token = this.tokenizer.get();
//			System.out.println( "Token: " + token );
		}
		while( token.isValueSeparator() );

		if( !token.isEndObject() )
			throw new CommandFileException( "Expecting , or }, not '" + token + "'", this.tokenizer.getLocation() );

		return result;
	}

	public JSONArray readArray()
	{
		JSONArray result = new JSONArray();

		Token token;
		do
		{
			Object value = read();
			result.add( value );

			token = this.tokenizer.get();
//			System.out.println( "Token: " + token );
		}
		while( token.isValueSeparator() );

		if( !token.isEndArray() )
			throw new CommandFileException( "Expecting , or ], not '" + token + "'", this.tokenizer.getLocation() );

		return result;
	}

	public JSONObject readProperties()
	{
		JSONObject result = new JSONObject();

		Token token = this.tokenizer.get( true );
		while( token.isString() )
		{
			String name = (String)token.getValue();
			token = this.tokenizer.get();
			if( !token.isNameSeparator() )
				throw new CommandFileException( "Expecting :, not '" + token + "'", this.tokenizer.getLocation() );
			result.set( name, read() );

			token = this.tokenizer.get( true );
			if( !token.isNewLine() )
				throw new CommandFileException( "Expecting newline, not '" + token + "'", this.tokenizer.getLocation() );

			token = this.tokenizer.get( true );
		}

		if( !token.isNewLine() )
			throw new CommandFileException( "Expecting empty line or string, not '" + token + "'", this.tokenizer.getLocation() );

		return result;
	}

	public JSONArray readValues()
	{
		JSONArray result = new JSONArray();

		try
		{
			Object value = read();
			result.add( value );
		}
		catch( JSONEndOfInputException e )
		{
			return null;
		}

		Token token = this.tokenizer.get( true );
		while( token.isValueSeparator() )
		{
			Object value = read();
			result.add( value );

			token = this.tokenizer.get( true );
		}

		if( !( token.isNewLine() || token.isEndOfInput() ) )
			throw new CommandFileException( "Expecting newline or end-of-input, not '" + token + "'", this.tokenizer.getLocation() );

		return result;
	}

	/**
	 * Returns the current line number. The line number is the number of the line of data about to be read.
	 *
	 * @return The current line number.
	 */
	public int getLineNumber()
	{
		return this.tokenizer.getLineNumber();
	}

	public FileLocation getLocation()
	{
		return this.tokenizer.getLocation();
	}

	public void close()
	{
		this.tokenizer.close();
	}
}
