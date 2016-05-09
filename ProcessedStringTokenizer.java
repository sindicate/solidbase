/*--
 * Copyright 2012 René M. de Bloois
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

package solidbase.core.script;

import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;


/**
 * A tokenizer for a super string.
 *
 * @author René de Bloois
 */
/*
Scala:
SimpleExpr1  ::= ... | processedStringLiteral
processedStringLiteral
             ::= alphaid`"' {printableChar \ (`"' | `$') | escape} `"'
              |  alphaid `"""' {[`"'] [`"'] char \ (`"' | `$') | escape} {`"'} `"""'
escape       ::= `$$'
              |  `$' letter { letter | digit }
              |  `$'BlockExpr
alphaid      ::=  upper idrest
              |  varid
 */
// TODO Include 'raw' and 'f'
public class ProcessedStringTokenizer
{
	private SourceReader in;
	private StringBuilder buffer = new StringBuilder( 256 );
	private boolean found;


	/**
	 * @param in The source reader.
	 */
	public ProcessedStringTokenizer( SourceReader in )
	{
		this.in = in;
	}

	private StringBuilder clearBuffer()
	{
		StringBuilder buffer = this.buffer;
		buffer.setLength( 0 );
		return buffer;
	}

	/**
	 * Read a string fragment. A fragment ends at a ${ or at the end of the string. After calling this method the method
	 * {@link #foundExpression()} indicates if an ${ expression was encountered while reading the last fragment.
	 *
	 * @return The fragment. Maybe empty but never null.
	 */
	public Fragment getFragment()
	{
		this.found = false;
		SourceReader in = this.in;
		SourceLocation location = in.getLocation();

		in.record();

		while( true )
		{
			int ch = in.read();
			switch( ch )
			{
				case -1:
					return new Fragment( location, in.getRecorded() ); // end-of-input: we're done

				case '$':
					int ch2 = in.read();
					if( ch2 == '{' )
					{
						this.found = true;
						in.rewind();
						in.rewind();
						String result = in.getRecorded();
						in.read();
						in.read();
						return new Fragment( location, result );
					}
					if( ch2 != '$' )
						throw new SourceException( "$ must start an escape like ${...} or $$", in.getLocation() );
					break;
			}
		}
	}

	/**
	 * @return True if a ${ expression was found while reading the last fragment.
	 */
	public boolean foundExpression()
	{
		return this.found;
	}
}
