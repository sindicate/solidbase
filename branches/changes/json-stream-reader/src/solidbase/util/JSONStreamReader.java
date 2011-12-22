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

import java.util.Stack;

import solidbase.core.CommandFileException;
import solidbase.util.JSONTokenizer.Token;


/**
 * Reads JSON data from the given {@link LineReader}.
 *
 * @author René M. de Bloois
 */
public class JSONStreamReader
{
	static public enum EVENT { BEGIN_OBJECT, END_OBJECT, NAME, VALUE, BEGIN_ARRAY, END_ARRAY };
	static protected enum STATE { VALUE, NAME, ENDVALUE };
	static protected enum STRUCT { NONE, OBJECT, ARRAY };

	/**
	 * The source of tokens.
	 */
	private JSONTokenizer tokenizer;

	protected STATE state = STATE.VALUE;
	protected STRUCT currentStruct = STRUCT.NONE;
	protected Stack< STRUCT > pastStructs = new Stack< STRUCT >();

	protected String name;
	protected Object value;


	/**
	 * Constructor.
	 *
	 * @param reader The source of the JSON data.
	 */
	public JSONStreamReader( LineReader reader )
	{
		this.tokenizer = new JSONTokenizer( reader );
	}

	public EVENT next()
	{
		// clear everything
		this.name = null;
		this.value = null;

		JSONTokenizer tokenizer = this.tokenizer;
		Token token = tokenizer.get();

		if( this.state == STATE.ENDVALUE )
		{
			if( this.currentStruct == STRUCT.OBJECT )
			{
				if( token.isValueSeparator() )
				{
					this.state = STATE.NAME;
					token = tokenizer.get();
				}
				else if( token.isEndObject() )
				{
					this.currentStruct = this.pastStructs.pop(); // TODO EmptyStack
					return EVENT.END_OBJECT;
				}
				else
					throw new CommandFileException( "Expecting , or }, not '" + token + "'", tokenizer.getLocation() );
			}
			else if( this.currentStruct == STRUCT.ARRAY )
			{
				if( token.isValueSeparator() )
				{
					this.state = STATE.VALUE;
					token = tokenizer.get();
				}
				else if( token.isEndArray() )
				{
					this.currentStruct = this.pastStructs.pop(); // TODO EmptyStack
					return EVENT.END_OBJECT;
				}
				else
					throw new CommandFileException( "Expecting , or ], not '" + token + "'", tokenizer.getLocation() );
			}
			else
				throw new JSONEndOfInputException();
		}

		// TODO Use switch(), need an enum for this
		if( this.state == STATE.VALUE )
		{
			if( token.isString() || token.isNumber() || token.isNull() || token.isBoolean() )
			{
				this.value = token.getValue();
				this.state = STATE.ENDVALUE;
				return EVENT.VALUE;
			}
			if( token.isBeginObject() )
			{
				this.pastStructs.push( this.currentStruct );
				this.currentStruct = STRUCT.OBJECT;
				this.state = STATE.NAME;
				return EVENT.BEGIN_OBJECT;
			}
			if( token.isBeginArray() )
			{
				this.pastStructs.push( this.currentStruct );
				this.currentStruct = STRUCT.ARRAY;
				this.state = STATE.VALUE;
				return EVENT.BEGIN_ARRAY;
			}
			throw new CommandFileException( "Expecting {, [, \", a number, true, false or null, not '" + token + "'", tokenizer.getLocation() );
		}

		if( this.state == STATE.NAME )
		{
			if( token.isString() )
			{
				this.name = (String)token.getValue();
				token = tokenizer.get();
				if( !token.isNameSeparator() )
					throw new CommandFileException( "Expecting :, not '" + token + "'", tokenizer.getLocation() );
				this.state = STATE.VALUE;
				return EVENT.NAME;
			}
			throw new CommandFileException( "Expecting \", not '" + token + "'", tokenizer.getLocation() );
		}

		throw new CommandFileException( "Expecting {, [, \", a number, true, false or null, not '" + token + "'", tokenizer.getLocation() );
	}

	public EVENT next( EVENT required )
	{
		EVENT event = next();
		if( event != required )
			// TODO eventToString()
			throw new CommandFileException( "Expected " + required + ", not " + event, getLocation() );
		return event;
	}

	public String getName()
	{
		return this.name;
	}

	public Object getValue()
	{
		return this.value;
	}

	public FileLocation getLocation()
	{
		return this.tokenizer.getLocation();
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

	public void close()
	{
		this.tokenizer.close();
	}
}
