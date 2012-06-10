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

import solidbase.core.SourceException;
import solidbase.util.JSONTokenizer.Token;
import solidbase.util.JSONTokenizer.Token.TYPE;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.io.Resource;


/**
 * Reads JSON data from the given {@link SourceReader}.
 *
 * @author René M. de Bloois
 */
public class JSONParser
{
	static public enum EVENT { BEGIN_OBJECT, END_OBJECT, BEGIN_ARRAY, END_ARRAY, NAME, VALUE, EOF };
	static protected enum STATE { BEFOREVALUE, BEFORENAME, AFTERVALUE };
	static protected enum STRUCT { NONE, OBJECT, ARRAY };

	private SourceReader reader;

	/**
	 * The source of tokens.
	 */
	private JSONTokenizer tokenizer;

	// Parsing state
	protected STATE state = STATE.BEFOREVALUE;
	protected STRUCT currentStruct = STRUCT.NONE;
	protected Stack< STRUCT > pastStructs = new Stack< STRUCT >();

	// Parsed names and values
	protected String name;
	protected Object value;


	/**
	 * Constructor.
	 *
	 * @param reader The source of the JSON data.
	 */
	public JSONParser( SourceReader reader )
	{
		this.reader = reader;
		this.tokenizer = new JSONTokenizer( reader );
	}

	public EVENT next()
	{
		// clear everything
		this.name = null;
		this.value = null;

		JSONTokenizer tokenizer = this.tokenizer;
		Token token = tokenizer.get();

		while( true )
			switch( this.state )
			{
				case AFTERVALUE:
					switch( this.currentStruct )
					{
						case OBJECT:
							switch( token.getType() )
							{
								case VALUE_SEPARATOR:
									this.state = STATE.BEFORENAME;
									token = tokenizer.get();
									continue;
								case END_OBJECT:
									this.currentStruct = this.pastStructs.pop(); // TODO EmptyStack
									return EVENT.END_OBJECT;
								default:
									throw new SourceException( "Expecting , or }, not '" + token + "'", tokenizer.getLocation() );
							}
						case ARRAY:
							switch( token.getType() )
							{
								case VALUE_SEPARATOR:
									this.state = STATE.BEFOREVALUE;
									token = tokenizer.get();
									continue;
								case END_ARRAY:
									this.currentStruct = this.pastStructs.pop(); // TODO EmptyStack
									return EVENT.END_OBJECT;
								default:
									throw new SourceException( "Expecting , or ], not '" + token + "'", tokenizer.getLocation() );
							}
						case NONE:
							// Multiple top level objects are allowed, fall through to BEFOREVALUE
					} //$FALL-THROUGH$

				case BEFOREVALUE:
					switch( token.getType() )
					{
						case STRING:
						case NUMBER:
						case BOOLEAN:
						case NULL:
							this.value = token.getValue();
							this.state = STATE.AFTERVALUE;
							return EVENT.VALUE;
						case BEGIN_OBJECT:
							this.pastStructs.push( this.currentStruct );
							this.currentStruct = STRUCT.OBJECT;
							this.state = STATE.BEFORENAME;
							return EVENT.BEGIN_OBJECT;
						case BEGIN_ARRAY:
							this.pastStructs.push( this.currentStruct );
							this.currentStruct = STRUCT.ARRAY;
							this.state = STATE.BEFOREVALUE;
							return EVENT.BEGIN_ARRAY;
						case EOF:
							if( this.currentStruct == STRUCT.NONE )
								return EVENT.EOF;
							//$FALL-THROUGH$
						default:
							// TODO We have 2 kinds of EOF (are we in a STRUCT or not?)
							throw new SourceException( "Expecting {, [, \", a number, true, false or null, not '" + token + "'", tokenizer.getLocation() );
					}

				case BEFORENAME:
					if( token.getType() != TYPE.STRING )
						throw new SourceException( "Expecting \", not '" + token + "'", tokenizer.getLocation() );
					this.name = (String)token.getValue();
					token = tokenizer.get();
					if( token.getType() != TYPE.NAME_SEPARATOR )
						throw new SourceException( "Expecting :, not '" + token + "'", tokenizer.getLocation() );
					this.state = STATE.BEFOREVALUE;
					return EVENT.NAME;
			}
	}

	public EVENT next( EVENT required )
	{
		EVENT event = next();
		if( event != required )
			// TODO eventToString()
			throw new SourceException( "Expected " + required + ", not " + event, getLocation() );
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

	public SourceLocation getLocation()
	{
		return this.tokenizer.getLocation();
	}

	public Resource getResource()
	{
		return this.reader.getResource();
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
