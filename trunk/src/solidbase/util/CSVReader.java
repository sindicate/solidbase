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

import java.util.ArrayList;
import java.util.List;

import solidbase.core.SourceException;
import solidbase.util.CSVTokenizer.Token;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;


/**
 * Reads CSV data from the given {@link SQLTokenizer}.
 *
 * @author René M. de Bloois
 */
public class CSVReader
{
	/**
	 * The source of tokens.
	 */
	protected CSVTokenizer tokenizer;

	/**
	 * The separator that separates the values.
	 */
	protected char separator;


	/**
	 * Constructor.
	 *
	 * @param reader The source of the CSV data.
	 * @param separator The separator that separates the values.
	 * @param ignoreWhiteSpace Ignore white space, except white space enclosed in double quotes.
	 */
	public CSVReader( SourceReader reader, char separator, boolean ignoreWhiteSpace )
	{
		this.tokenizer = new CSVTokenizer( reader, separator, ignoreWhiteSpace );
		this.separator = separator;
	}

	/**
	 * Gets a line of values from the CSV data.
	 *
	 * @return A line of values from the CSV data.
	 */
	public String[] getLine()
	{
		CSVTokenizer tokenizer = this.tokenizer;
		List< String > values = new ArrayList< String >();

		while( true )
		{
			Token token = tokenizer.get();

			// We expect a value here. So if we get a separator/newline/EOI then we need to add an empty value
			if( token.isSeparator() )
			{
				values.add( "" );
			}
			else if( token.isNewline() || token.isEndOfInput() )
			{
				if( values.size() > 0 ) // Only if values are already found
					values.add( "" );
				break;
			}
			else
			{
				values.add( token.getValue() );
				token = tokenizer.get();
				if( token.isNewline() || token.isEndOfInput() )
					break;
				if( !token.isSeparator() )
					throw new SourceException( "Expecting <separator>, <newline> or <end-of-input>, not '" + token.getValue() + "'", tokenizer.getLocation() );
			}
		}

		if( values.isEmpty() )
			return null;
		return values.toArray( new String[ values.size() ] );
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

	public SourceLocation getLocation()
	{
		return this.tokenizer.getLocation();
	}
}
