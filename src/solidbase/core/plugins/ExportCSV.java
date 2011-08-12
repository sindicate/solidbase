/*--
 * Copyright 2011 René M. de Bloois
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

package solidbase.core.plugins;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.SystemException;
import solidbase.util.Resource;
import solidbase.util.StringLineReader;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author René M. de Bloois
 * @since Aug 12, 2011
 */
public class ExportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "EXPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		Parsed parsed = parse( command );

		Resource resource = processor.getResource().createRelative( parsed.fileName );
		Writer out;
		try
		{
			out = new OutputStreamWriter( resource.getOutputStream(), parsed.encoding );
		}
		catch( UnsupportedEncodingException e )
		{
			// toString() instead of getMessage(), the getMessage only returns the encoding string
			throw new CommandFileException( e.toString(), command.getLineNumber() );
		}
		try
		{
			try
			{
				Connection connection = processor.getCurrentDatabase().getConnection();
				Statement statement = processor.createStatement( connection );
				try
				{
					ResultSet result = statement.executeQuery( parsed.query );

					ResultSetMetaData metaData = result.getMetaData();
					int count = metaData.getColumnCount();
					int[] types = new int[ count ];
					String[] names = new String[ count ];
					boolean[] coalesce = new boolean[ count ];
					int firstCoalesce = -1;

					boolean first = true;
					for( int i = 0; i < count; i++ )
					{
						String name = metaData.getColumnName( i + 1 );
						types[ i ] = metaData.getColumnType( i + 1 );
						names[ i ] = name;
						if( parsed.coalesce != null && parsed.coalesce.contains( name.toUpperCase() ) )
						{
							coalesce[ i ] = true;
							if( firstCoalesce < 0 )
								firstCoalesce = i;
						}
						if( !coalesce[ i ] || firstCoalesce == i )
						{
							if( first )
								first = false;
							else
								out.write( ',' );
							out.write( name );
						}
					}

					out.write( '\n' );

					while( result.next() )
					{
						Object coalescedValue = null;
						int coalescedType = 0;
						if( parsed.coalesce != null )
							for( int i = 0; i < count; i++ )
								if( coalesce[ i ] )
								{
									coalescedValue = result.getObject( i + 1 );
									if( coalescedValue != null )
									{
										coalescedType = types[ i ];
										break;
									}
								}

						first = true;
						for( int i = 0; i < count; i++ )
						{
							if( !coalesce[ i ] || firstCoalesce == i )
							{
								if( first )
									first = false;
								else
									out.write( ',' );

								int type = firstCoalesce == i ? coalescedType : types[ i ];
								Object value = firstCoalesce == i ? coalescedValue : result.getObject( i + 1 );

								if( value != null )
									if( type == Types.CLOB || type == Types.NCLOB || type == Types.LONGVARCHAR || type == Types.LONGNVARCHAR )
									{
										Reader in = ( (Clob)value ).getCharacterStream();
										out.write( '"' );
										char[] buf = new char[ 4096 ];
										for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
											out.write( new String( buf, 0, read ).replace( "\"", "\"\"" ) );
										out.write( '"' );
										in.close();
									}
									else
									{
										String s = value.toString();
										boolean needQuotes = Pattern.compile( "[,\"\r\n]" ).matcher( s ).find();
										if( needQuotes )
										{
											s = s.replace( "\"", "\"\"" );
											out.write( '"' );
										}
										out.write( s );
										if( needQuotes )
											out.write( '"' );
									}
							}
						}

						out.write( '\n' );
					}
				}
				finally
				{
					statement.close();
					// TODO This is not right, we should check autocommit
					connection.commit();
				}
			}
			finally
			{
				out.close();
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		return true;
	}


	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	static protected Parsed parse( Command command )
	{
		Parsed result = new Parsed();

		Tokenizer tokenizer = new Tokenizer( new StringLineReader( command.getCommand(), command.getLineNumber() ) );

		tokenizer.get( "EXPORT" );
		tokenizer.get( "CSV" );

		Token t = tokenizer.get( "COALESCE", "FILE" );
		if( t.equals( "COALESCE" ) )
		{
			result.coalesce = new HashSet< String >();
			do
			{
				t = tokenizer.get();
				result.coalesce.add( t.getValue().toUpperCase() );
				t = tokenizer.get();
			}
			while( t.equals( "," ) );
			tokenizer.push( t );

			tokenizer.get( "FILE" );
		}

		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLineNumber() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get( "ENCODING" );
		t = tokenizer.get();
		String encoding = t.getValue();
		if( !encoding.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting encoding enclosed in double quotes, not [" + t + "]", tokenizer.getLineNumber() );
		encoding = encoding.substring( 1, encoding.length() - 1 );

		String query = tokenizer.getRemaining();

		result.fileName = file;
		result.encoding = encoding;
		result.query = query;

		return result;
	}


	/**
	 * A parsed command.
	 *
	 * @author René M. de Bloois
	 */
	static protected class Parsed
	{
		/** Skip the header line. */
		protected boolean skipHeader = false;

//		/** The separator. */
//		protected char separator = ',';

		/** The file path to export to */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;

		/** The query */
		protected String query;

		/** Which columns need to be coalesced */
		protected Set< String > coalesce;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
