/*--
 * Copyright 2012 Ren� M. de Bloois
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import solidstack.io.FatalIOException;


/**
 * A CSV writer.
 *
 * @author Ren� de Bloois
 */
public class CSVWriter
{
	private Writer out;
	private char separator;
	private Pattern needQuotesPattern;
	private boolean valueWritten;


	/**
	 * @param out A writer.
	 * @param separator The value separator.
	 * @param extendedFormat True if extended format needed.
	 */
	public CSVWriter( Writer out, char separator )
	{
		Assert.isFalse( separator == '"', "Double quote (\") not allowed as value separator" );

		this.out = out;
		this.separator = separator;

		// Pattern: ", CR, NL or parsed.separator, or ^ when extended format is enabled
		this.needQuotesPattern = Pattern.compile( "\"|\r|\n|" + Pattern.quote( Character.toString( separator ) ) );
	}

	/**
	 * Write a value.
	 *
	 * @param value The value to write.
	 */
	public void writeValue( String value )
	{
		writeSeparatorIfNeeded();
		internalWriteValue( value );
	}

	private void internalWriteValue( String value )
	{
		if( value == null )
			return;
		try
		{
			boolean needQuotes = this.needQuotesPattern.matcher( value ).find();
			if( needQuotes )
			{
				this.out.write( '"' );
				int len = value.length();
				for( int i = 0; i < len; i++ )
				{
					char c = value.charAt( i );
					if( c == '"' )
						this.out.write( c );
					this.out.write( c );
				}
				this.out.write( '"' );
			}
			else
				this.out.write( value );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	/**
	 * Write the contents of the reader as a value to the CSV.
	 *
	 * @param reader The reader to write to the CSV.
	 */
	public void writeValue( Reader reader )
	{
		writeSeparatorIfNeeded();
		try
		{
			this.out.write( '"' );
			char[] buf = new char[ 4096 ];
			for( int read = reader.read( buf ); read >= 0; read = reader.read( buf ) )
				this.out.write( new String( buf, 0, read ).replace( "\"", "\"\"" ) );
			this.out.write( '"' );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	private void nextValue()
	{
		try
		{
			this.out.write( this.separator );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	private void writeSeparatorIfNeeded()
	{
		if( this.valueWritten )
			nextValue();
		this.valueWritten = true;
	}

	/**
	 * Start the next record. This writes a newline.
	 */
	public void nextRecord()
	{
		this.valueWritten = false;
		try
		{
			this.out.write( '\n' );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	/**
	 * Close the CSV writer.
	 */
	public void close()
	{
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}
}
