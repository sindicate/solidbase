/*--
 * Copyright 2005 René M. de Bloois
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.SystemException;


/**
 * A line reader that automatically detects character encoding through the BOM and the first line.
 * 
 * @author René M. de Bloois
 */
public class BOMDetectingLineReader extends LineReader
{
	/**
	 * Constant for the ISO-8859-1 character set.
	 */
	static final public String CHARSET_ISO = "ISO-8859-1";

	/**
	 * Constant for the UTF-8 character set.
	 */
	static final public String CHARSET_UTF8 = "UTF-8";

	/**
	 * Constant for the UTF-16BE character set.
	 */
	static final public String CHARSET_UTF16BE = "UTF-16BE";

	/**
	 * Constant for the UTF-16LE character set.
	 */
	static final public String CHARSET_UTF16LE = "UTF-16LE";

	/**
	 * Constant for the default character set, which is ISO-8859-1.
	 */
	static final public String CHARSET_DEFAULT = CHARSET_ISO;

	/**
	 * The encoding of the stream.
	 */
	protected String encoding = CHARSET_DEFAULT;

	/**
	 * The Byte Order Mark found at the beginning of the stream. Null if not present.
	 */
	protected byte[] bom;


	/**
	 * Creates a new line reader for the given input streamL.
	 * 
	 * @param in The input stream to read lines from.
	 * @param encodingDetection A regular expression to detect the encoding on the first line.
	 */
	public BOMDetectingLineReader( BufferedInputStream in, Pattern encodingDetection )
	{
		try
		{
			in.mark( 1000 );
			byte[] bytes = new byte[] { 2, 2, 2, 2 };
			in.read( bytes ); // No need to know how many bytes have been read
			in.reset();

			// BOMS:
			// 00 00 FE FF  UTF-32, big-endian
			// FF FE 00 00 	UTF-32, little-endian
			// FE FF 	    UTF-16, big-endian
			// FF FE 	    UTF-16, little-endian
			// EF BB BF 	UTF-8
			if( bytes[ 0 ] == -17 && bytes[ 1 ] == -69 && bytes[ 2 ] == -65 )
			{
				this.encoding = CHARSET_UTF8;
				this.bom = new byte[] { -17, -69, -65 };
			}
			else if( bytes[ 0 ] == -2 && bytes[ 1 ] == -1 )
			{
				this.encoding = CHARSET_UTF16BE;
				this.bom = new byte[] { -2, -1 };
			}
			else if( bytes[ 0 ] == -1 && bytes[ 1 ] == -2 )
			{
				this.encoding = CHARSET_UTF16LE;
				this.bom = new byte[] { -1, -2 };
			}

			if( this.bom != null )
				Assert.isTrue( in.skip( this.bom.length ) == this.bom.length );

			this.reader = new BufferedReader( new InputStreamReader( in, this.encoding ) );
			this.currentLineNumber = 1;

			// BOM is skipped, reader created, line number = 1

			if( encodingDetection != null )
			{
				this.reader.mark( 1000 );
				String firstLine = this.reader.readLine();
				this.reader.reset();

				if( firstLine != null )
				{
					// Remove zeroes
					StringBuilder s = new StringBuilder();
					for( char c : firstLine.toCharArray() )
						if( c != 0 )
							s.append( c );

					Matcher matcher = encodingDetection.matcher( s.toString() );
					if( matcher.matches() )
					{
						if( !this.encoding.equalsIgnoreCase( matcher.group( 1 ) ) )
						{
							this.encoding = matcher.group( 1 );
							in.reset();
							if( this.bom != null )
								Assert.isTrue( in.skip( this.bom.length ) == this.bom.length );
							this.reader = new BufferedReader( new InputStreamReader( in, this.encoding ) );
						}
						this.reader.readLine();
						this.currentLineNumber = 2;

						// BOM is skipped, reader created, line number = 2
					}
				}
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Returns the current character encoding of the stream.
	 * 
	 * @return The current character encoding of the stream.
	 */
	public String getEncoding()
	{
		return this.encoding;
	}

	/**
	 * Returns the Byte Order Mark found in the stream.
	 * 
	 * @return The Byte Order Mark found. Will be null if no BOM was present.
	 */
	public byte[] getBOM()
	{
		return this.bom;
	}
}
