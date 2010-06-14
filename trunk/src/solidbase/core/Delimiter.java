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

package solidbase.core;

import java.util.regex.Pattern;


/**
 * A command delimiter.
 * 
 * @author René M. de Bloois
 */
class Delimiter
{
	/**
	 * The possible types of command delimiter.
	 * 
	 * @author René M. de Bloois
	 */
	static protected enum Type
	{
		/**
		 * The delimiter can be anywhere in the line.
		 */
		FREE,
		/**
		 * The delimiter must be at the end of the line (or in its own line, obviously).
		 */
		TRAILING,
		/**
		 * The delimiter must be in a line of its own.
		 */
		ISOLATED
	}

	/**
	 * The text of the delimiter.
	 */
	protected String text;

	/**
	 * The type of the delimiter.
	 */
	protected Delimiter.Type type;

	/**
	 * A regular expression that find the delimiter.
	 */
	protected Pattern pattern;

	/**
	 * Constructor for the delimiter.
	 * 
	 * @param text The text of the delimiter.
	 * @param type The type of the delimiter.
	 */
	protected Delimiter( String text, Delimiter.Type type )
	{
		this.text = text;
		this.type = type;
		if( type == Type.ISOLATED )
			this.pattern = Pattern.compile( "\\s*" + Pattern.quote( text ) + "\\s*" );
		else if( type == Type.TRAILING )
			this.pattern = Pattern.compile( "(.*)" + Pattern.quote( text ) + "\\s*" );
		else
			this.pattern = Pattern.compile( "(.*?)" + Pattern.quote( text ) + "\\s*(.+)?" );
	}

	@Override
	public String toString()
	{
		return this.text + " " + this.type;
	}
}
