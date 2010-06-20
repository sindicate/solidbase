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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;

import solidbase.core.Delimiter.Type;


/**
 * Source for SQL statements.
 * 
 * @author René M. de Bloois
 * @since June 2010
 */
public class SQLSource implements CommandSource
{
	/**
	 * The default default delimiter: GO with type {@link Type#ISOLATED}.
	 */
	static protected final Delimiter[] DEFAULT_DELIMITERS = new Delimiter[] { new Delimiter( ";", Type.TRAILING ) };

	/**
	 * The underlying reader.
	 */
	protected BufferedReader reader;

	/**
	 * A buffer needed when a delimiter is used of type {@link Type#FREE}.
	 */
	protected String buffer;

	/**
	 * The default delimiters.
	 */
	protected Delimiter[] defaultDelimiters = DEFAULT_DELIMITERS;

	/**
	 * Temporary delimiters.
	 */
	protected Delimiter[] delimiters = null;

	/**
	 * Current line number.
	 */
	protected int lineNumber;


	/**
	 * Creates a new instance of an SQL source.
	 * 
	 * @param in The reader which is used to read the SQL.
	 */
	protected SQLSource( Reader in )
	{
		if( in instanceof BufferedReader )
			this.reader = (BufferedReader)in;
		else
			this.reader = new BufferedReader( in );
		this.lineNumber = 0;
	}


	/**
	 * Creates a new instance of an SQL source.
	 * 
	 * @param sql The SQL to read.
	 */
	protected SQLSource( String sql )
	{
		this( new StringReader( sql ) );
	}


	/**
	 * Creates a new instance of an SQL source.
	 * 
	 * @param sql The SQL to read.
	 * @param lineNumber The line number of the SQL within the original file.
	 */
	protected SQLSource( String sql, int lineNumber )
	{
		this( new StringReader( sql ) );
		this.lineNumber = lineNumber - 1;
	}


	/**
	 * Close the patch file. This will also close the underlying file.
	 */
	public void close()
	{
		try
		{
			this.reader.close();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}


	/**
	 * Sets the default delimiters.
	 * 
	 * @param delimiters The delimiters.
	 */
	protected void setDefaultDelimiters( Delimiter[] delimiters )
	{
		this.defaultDelimiters = delimiters;
	}


	/**
	 * Overrides the default delimiter.
	 * 
	 * @param delimiters The delimiters.
	 */
	public void setDelimiters( Delimiter[] delimiters )
	{
		this.delimiters = delimiters;
	}


	/**
	 * Reads a command from the patch file.
	 * 
	 * @return A command from the patch file or null when no more commands are available.
	 */
	public Command readCommand()
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
					line = this.reader.readLine();
					this.lineNumber++;
					if( line == null )
					{
						if( result.length() > 0 )
							throw new UnterminatedStatementException( this.lineNumber );
						return null;
					}

					if( line.startsWith( "--*" ) ) // Only if read from file
					{
						if( result.length() > 0 )
							throw new UnterminatedStatementException( this.lineNumber );

						line = line.substring( 3 ).trim();
						if( !line.startsWith( "//" )) // skip comment
						{
							if( pos == 0 )
								pos = this.lineNumber;
							return new Command( line, true, pos );
						}
						continue;
					}
				}

				if( pos == 0 && line.trim().length() == 0 ) // Skip the first empty lines
					continue;

				for( Delimiter delimiter : this.delimiters != null ? this.delimiters : this.defaultDelimiters )
				{
					Matcher matcher = delimiter.pattern.matcher( line );
					if( matcher.matches() )
					{
						if( pos == 0 )
							pos = this.lineNumber;
						if( matcher.groupCount() > 0 )
							result.append( matcher.group( 1 ) );
						if( matcher.groupCount() > 1 )
							this.buffer = matcher.group( 2 );
						return new Command( result.toString(), false, pos );
					}
				}

				if( pos == 0 )
					pos = this.lineNumber;
				result.append( line );
				result.append( '\n' );
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
	}
}
