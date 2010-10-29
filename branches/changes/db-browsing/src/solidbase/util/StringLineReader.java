/*--
 * Copyright 2005 Ren� M. de Bloois
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

import java.io.BufferedReader;
import java.io.StringReader;


/**
 * A line reader that reads from a string.
 * 
 * @author Ren� M. de Bloois
 */
public class StringLineReader extends LineReader
{
	/**
	 * Creates a new line reader for the given input stream.
	 * 
	 * @param text The text to read from.
	 */
	public StringLineReader( String text )
	{
		this.reader = new BufferedReader( new StringReader( text ) );
	}

	/**
	 * Creates a new line reader for the given input stream.
	 * 
	 * @param text The text to read from.
	 * @param lineNumber The line number of the text fragment in the file.
	 */
	public StringLineReader( String text, int lineNumber )
	{
		this.reader = new BufferedReader( new StringReader( text ) );
		this.currentLineNumber = lineNumber;
	}
}
