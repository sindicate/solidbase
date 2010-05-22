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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Delimiter.Type;
import solidbase.util.RandomAccessLineReader;


/**
 * This class manages an SQL file's contents. It detects the encoding and reads commands from it.
 * 
 * @author René M. de Bloois
 * @since Apr 2010
 */
public class SQLFile
{
	static private final Pattern ENCODING_PATTERN = Pattern.compile( "^--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\"[ \t]*$", Pattern.CASE_INSENSITIVE );

	/**
	 * The default delimiter: GO with type {@link Type#SEPARATELINE}.
	 */
	static protected final Delimiter[] DEFAULT_DELIMITERS = new Delimiter[] { new Delimiter( "GO", Type.SEPARATELINE ) };

	/**
	 * The underlying file.
	 */
	protected RandomAccessLineReader file;

	/**
	 * A buffer needed when a delimiter is used of type {@link Type#FREE}.
	 */
	protected String buffer;

	/**
	 * Delimiters that override the default delimiter.
	 */
	protected Delimiter[] delimiters = null;


	/**
	 * Creates an new instance of an sql file.
	 * 
	 * @param file The reader which is used to read the contents of the file.
	 */
	protected SQLFile( RandomAccessLineReader file )
	{
		this.file = file;

		try
		{
			String line = file.readLine();
			//System.out.println( "First line [" + line + "]" );
			StringBuilder s = new StringBuilder();
			char[] chars = line.toCharArray();
			for( char c : chars )
				if( c != 0 )
					s.append( c );

			line = s.toString();
			//System.out.println( "First line (fixed) [" + line + "]" );
			Matcher matcher = ENCODING_PATTERN.matcher( line );
			if( matcher.matches() )
			{
				file.reOpen( matcher.group( 1 ) );
				file.readLine(); // skip the first line
			}
			else
				file.gotoLine( 1 );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}


	/**
	 * Close the patch file. This will also close the underlying file.
	 */
	protected void close()
	{
		if( this.file != null )
		{
			try
			{
				this.file.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			this.file = null;
		}
	}


	/**
	 * Clears out all delimiters, causing the default delimiter to be used: {@link #DEFAULT_DELIMITERS}.
	 */
	public void resetDelimiters()
	{
		this.delimiters = null;
	}


	/**
	 * Add a delimiter.
	 * 
	 * @param delimiter The delimiter to add.
	 */
	public void addDelimiter( Delimiter delimiter )
	{
		if( this.delimiters == null )
			this.delimiters = new Delimiter[] { delimiter };
		else
		{
			int len = this.delimiters.length;
			Delimiter[] d = new Delimiter[ len + 1 ];
			System.arraycopy( this.delimiters, 0, d, 0, len );
			d[ len ] = delimiter;
			this.delimiters = d;
		}
	}


	/**
	 * Reads a command from the patch file.
	 * 
	 * @return A command from the patch file or null when no more commands are available.
	 */
	protected Command readStatement()
	{
		StringBuilder result = new StringBuilder();
		int pos = 0; // No line found yet

		while( true )
		{
			try
			{
				String line;
				if( this.buffer != null )
				{
					line = this.buffer;
					this.buffer = null;
				}
				else
				{
					line = this.file.readLine();
					if( line == null )
					{
						if( result.length() > 0 )
							throw new UnterminatedStatementException( this.file.getLineNumber() - 1 );
						return null;
					}

					if( line.startsWith( "--*" ) ) // Only if read from file
					{
						if( result.length() > 0 )
							throw new UnterminatedStatementException( this.file.getLineNumber() - 1 );

						line = line.substring( 3 ).trim();
						if( !line.startsWith( "//" )) // skip comment
						{
							if( pos == 0 )
								pos = this.file.getLineNumber() - 1;
							return new Command( line, true, pos );
						}
						continue;
					}
				}

				if( pos == 0 && line.trim().length() == 0 ) // Skip the first empty lines
					continue;

				for( Delimiter delimiter : this.delimiters != null ? this.delimiters : DEFAULT_DELIMITERS )
				{
					Matcher matcher = delimiter.pattern.matcher( line );
					if( matcher.matches() )
					{
						if( pos == 0 )
							pos = this.file.getLineNumber() - 1;
						if( matcher.groupCount() > 0 )
						{
							System.out.println( "1: " + matcher.group( 1 ) );
							result.append( matcher.group( 1 ) );
						}
						if( matcher.groupCount() > 1 )
						{
							System.out.println( "2: " + matcher.group( 2 ) );
							this.buffer = matcher.group( 2 );
						}
						return new Command( result.toString(), false, pos );
					}
				}

				if( pos == 0 )
					pos = this.file.getLineNumber() - 1;
				result.append( line );
				result.append( '\n' );
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
	}


	/**
	 * Gets the encoding of the patch file.
	 * 
	 * @return The encoding of the patch file.
	 */
	public String getEncoding()
	{
		return this.file.getEncoding();
	}
}
