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

import java.util.regex.Matcher;

import solidbase.core.Delimiter.Type;
import solidstack.io.FileLocation;
import solidstack.io.LineReader;
import solidstack.io.Resource;
import solidstack.io.StringLineReader;


/**
 * Source for SQL statements.
 *
 * @author René M. de Bloois
 * @since June 2010
 */
public class SQLSource
{
	/**
	 * The default default delimiter: GO with type {@link Type#ISOLATED}.
	 */
	static protected final Delimiter[] DEFAULT_DELIMITERS = new Delimiter[] { new Delimiter( ";", Type.TRAILING ) };

	/**
	 * The underlying reader.
	 */
	protected LineReader reader;

	/**
	 * A buffer needed when a delimiter is used of type {@link Type#FREE}.
	 */
	protected String buffer;

	/**
	 * Temporary delimiters.
	 */
	protected Delimiter[] delimiters = DEFAULT_DELIMITERS;


	/**
	 * Constructor.
	 *
	 * @param in The reader which is used to read the SQL.
	 */
	protected SQLSource( LineReader in )
	{
		this.reader = in;
	}


	/**
	 * Constructor.
	 *
	 * @param sql The SQL to read.
	 */
	protected SQLSource( String sql )
	{
		this( new StringLineReader( sql ) );
	}


	/**
	 * Constructor.
	 *
	 * @param sql The SQL to read.
	 * @param location The location of the SQL within the original file.
	 */
	protected SQLSource( String sql, FileLocation location )
	{
		this( new StringLineReader( sql, location ) );
	}


	/**
	 * Constructor.
	 *
	 * @param fragment The fragment of SQL from a file.
	 */
	protected SQLSource( Fragment fragment )
	{
		this( new StringLineReader( fragment.getText(), fragment.getLocation() ) );
	}


	/**
	 * Close the source. This will also close the underlying file.
	 */
	public void close()
	{
		if( this.reader != null )
		{
			this.reader.close();
			this.reader = null;
		}
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
	 * Reads a command from the upgrade segment.
	 *
	 * @return A command from the upgrade segment or null when no more commands are available.
	 */
	public Command readCommand()
	{
		StringBuilder result = new StringBuilder();
		int pos = 0; // No line found yet

		while( true )
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
				if( line == null )
				{
					if( result.length() > 0 )
						throw new NonDelimitedStatementException( this.reader.getLocation().previousLine() );
					return null;
				}

				if( line.startsWith( "--*" ) ) // Only if read from file
				{
					if( result.length() > 0 )
						throw new NonDelimitedStatementException( this.reader.getLocation().previousLine() );

					line = line.substring( 3 ).trim();
					if( !line.startsWith( "//" )) // skip comment
					{
						if( pos == 0 )
							pos = this.reader.getLineNumber() - 1;
						return new Command( line, true, this.reader.getLocation().lineNumber( pos ) );
					}
					continue;
				}
			}

			if( pos == 0 && line.trim().length() == 0 ) // Skip the first empty lines
				continue;

			for( Delimiter delimiter : this.delimiters )
			{
				Matcher matcher = delimiter.pattern.matcher( line );
				if( matcher.matches() )
				{
					if( pos == 0 )
						pos = this.reader.getLineNumber() - 1;
					if( matcher.groupCount() > 0 )
						result.append( matcher.group( 1 ) );
					if( matcher.groupCount() > 1 )
						this.buffer = matcher.group( 2 );
					return new Command( result.toString(), false, this.reader.getLocation().lineNumber( pos ) );
				}
			}

			if( pos == 0 )
				pos = this.reader.getLineNumber() - 1;
			result.append( line );
			result.append( '\n' );
		}
	}

	/**
	 * Returns the underlying resource.
	 *
	 * @return The underlying resource.
	 */
	public Resource getResource()
	{
		return this.reader.getResource();
	}
}
