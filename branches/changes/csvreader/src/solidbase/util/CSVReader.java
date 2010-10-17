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

import solidbase.util.CSVTokenizer.Token;


/**
 * Reads CSV data from the given {@link Tokenizer}.
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
	protected String separator;


	/**
	 * Constructor.
	 * 
	 * @param reader The source of the CSV data.
	 * @param separator The separator that separates the values.
	 */
	public CSVReader( LineReader reader, char separator )
	{
		this.tokenizer = new CSVTokenizer( reader, separator );
		this.separator = String.valueOf( separator );
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
		Token token = tokenizer.get();
//		System.out.println( "Token: " + token );
		while( !token.isEndOfInput() && !token.isNewline() )
		{
			if( token.equals( this.separator ) )
				values.add( "" );
			else
			{
				String value = token.getValue();
				if( value.startsWith( "\"" ) )
					value = value.substring( 1, value.length() - 1 );
				values.add( value );
				token = tokenizer.get( this.separator, "\n", null );
//				System.out.println( "Token: " + token );
			}
			if( token.equals( this.separator ) )
			{
				token = tokenizer.get();
//				System.out.println( "Token: " + token );
				if( token.isEndOfInput() || token.isNewline() )
					values.add( "" );
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
}
