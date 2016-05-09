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
import solidstack.io.SourceReader;


/**
 * Parses a funny script.
 *
 * @author René de Bloois
 *
 */
public class ScriptScanner
{
	private SourceReader in;


	public ScriptScanner( SourceReader reader )
	{
		this.in = reader;
	}

	/**
	 * Parses everything.
	 *
	 * @return An expression.
	 */
	public String scan()
	{
		this.in.record();

		SourceReader in = this.in;

		while( true )
		{
			int ch = in.read();
			switch( ch )
			{
				case '}':
					in.rewind();
					return this.in.getRecorded();

				case '"':
					string: while( true )
					{
						switch( ch = in.read() )
						{
							case -1: throw new SourceException( "Missing \"", in.getLocation() );
							case '"': break string;
							case '\n': throw new SourceException( "Unexpected LF", in.getLocation() );
							case '\r': throw new SourceException( "Unexpected CR", in.getLocation() );
							case '\\':
								switch( ch = in.read() )
								{
									case '\n': continue; // Skip newline
									case 'b': ch = '\b'; break;
									case 'f': ch = '\f'; break;
									case 'n': ch = '\n'; break;
									case 'r': ch = '\r'; break;
									case 't': ch = '\t'; break;
									case '"':
									case '\'':
									case '\\': break;
									case 'u': // TODO Actually, these escapes should be active through the entire script, like Java and Scala do. Maybe disabled by default. Or removed and optional for String literals.
										char[] codePoint = new char[ 4 ];
										for( int i = 0; i < 4; i++ )
										{
											codePoint[ i ] = Character.toUpperCase( (char)( ch = in.read() ) );
											if( !( ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' ) )
												throw new SourceException( "Illegal escape sequence: \\u" + new String( codePoint, 0, i + 1 ), in.getLastLocation() );
										}
										ch = Integer.valueOf( new String( codePoint ), 16 );
										break;
									default:
										throw new SourceException( "Illegal escape sequence: \\" + ( ch >= 0 ? (char)ch : "" ), in.getLastLocation() );
								}
						}
					}
			}
		}
	}
}
