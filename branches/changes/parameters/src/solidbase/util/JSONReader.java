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

import solidbase.core.SourceException;
import solidstack.io.SourceReader;


/**
 * Reads JSON data from the given {@link SourceReader}.
 *
 * @author René M. de Bloois
 */
public class JSONReader extends JSONParser
{
	private boolean endOfFile;

	/**
	 * Constructor.
	 *
	 * @param reader The source of the JSON data.
	 */
	// TODO Resource instead of SourceReader
	public JSONReader( SourceReader reader )
	{
		super( reader );
	}

	/**
	 * Gets a line of values from the JSON data.
	 *
	 * @return A line of values from the JSON data.
	 */
	public Object read()
	{
		// TODO Add switch()
		EVENT event = next();
		if( event == EVENT.VALUE )
			return getValue();
		if( event == EVENT.BEGIN_OBJECT )
			return readObject();
		if( event == EVENT.BEGIN_ARRAY )
			return readArray();
		if( event == EVENT.EOF )
		{
			this.endOfFile = true;
			return null;
		}

		throw new SourceException( "Expecting {, [, \", a number, true, false or null, not '" + event + "'", getLocation() );
	}

	public boolean isEOF()
	{
		return this.endOfFile;
	}

	public JSONObject readObject()
	{
		JSONObject result = new JSONObject();
		EVENT event = next();
		while( event == EVENT.NAME )
		{
			result.set( getName(), read() );
			event = next();
		}
		// event can only be an END_OBJECT here
		return result;
	}

	public JSONArray readArray()
	{
		JSONArray result = new JSONArray();
		while( true )
		{
			EVENT event = next();
			// TODO I do not really like this, a simple while in the readObject() but a complex while/switch in the readArray, need another event?
			switch( event )
			{
				case BEGIN_ARRAY:
					result.add( readArray() );
					continue;
				case BEGIN_OBJECT:
					result.add( readObject() );
					continue;
				case VALUE:
					result.add( getValue() );
					continue;
				default:
			}
			break;
		}
		// event can only be an END_ARRAY here
		return result;
	}
}
