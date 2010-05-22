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
	static private final Pattern GO_PATTERN = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );


	/**
	 * The underlying file.
	 */
	protected RandomAccessLineReader file;


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
				String line = this.file.readLine();
				if( line == null )
				{
					if( result.length() > 0 )
						throw new UnterminatedStatementException( this.file.getLineNumber() - 1 );
					return null;
				}

				if( pos == 0 && line.trim().length() == 0 ) // Skip the first empty lines
					continue;

				if( GO_PATTERN.matcher( line ).matches() )
				{
					if( pos == 0 )
						pos = this.file.getLineNumber() - 1;
					return new Command( result.toString(), false, pos );
				}

				if( line.startsWith( "--*" ) )
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
