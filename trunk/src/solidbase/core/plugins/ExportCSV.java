/*--
 * Copyright 2011 Ren� M. de Bloois
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import solidbase.core.Command;
import solidbase.core.SourceException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.SystemException;
import solidbase.core.plugins.DumpJSON.Coalescer;
import solidbase.util.CSVWriter;
import solidbase.util.JDBCSupport;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidstack.io.Resource;
import solidstack.io.Resources;
import solidstack.io.SourceReaders;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author Ren� M. de Bloois
 * @since Aug 12, 2011
 */
// TODO To compressed file
// TODO Escape with \ instead of doubling double quotes. This means also \n \t \r. ESCAPE DQ CR LF TAB WITH \
public class ExportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*EXPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		if( skip )
			return true;

		Parsed parsed = parse( command );

		Resource csvResource = Resources.getResource( parsed.fileName ); // Relative to current folder

		try
		{
			OutputStream out = csvResource.getOutputStream();
			if( parsed.gzip )
				out = new BufferedOutputStream( new GZIPOutputStream( out, 65536 ), 65536 ); // TODO Ctrl-C, close the outputstream?

			CSVWriter csvWriter;
			try
			{
				csvWriter = new CSVWriter( new OutputStreamWriter( out, parsed.encoding ), parsed.separator, false );
			}
			catch( UnsupportedEncodingException e )
			{
				// toString() instead of getMessage(), the getMessage only returns the character encoding
				throw new SourceException( e.toString(), command.getLocation() );
			}

			// TODO Lots of identical code in DumpJSON
			try
			{
				Statement statement = processor.createStatement();
				try
				{
					ResultSet result = statement.executeQuery( parsed.query );
					ResultSetMetaData metaData = result.getMetaData();

					// Define locals

					int columns = metaData.getColumnCount();
					int[] types = new int[ columns ];
					String[] names = new String[ columns ];
					boolean[] ignore = new boolean[ columns ];

					// Analyze metadata

					for( int i = 0; i < columns; i++ )
					{
						int col = i + 1;
						String name = metaData.getColumnName( col ).toUpperCase();
						types[ i ] = metaData.getColumnType( col );
						if( types[ i ] == Types.DATE && parsed.dateAsTimestamp )
							types[ i ] = Types.TIMESTAMP;
						names[ i ] = name;
						if( parsed.coalesce != null && parsed.coalesce.notFirst( name ) )
							ignore[ i ] = true;
						// TODO STRUCT serialize
						// TODO This must be optional and not the default
						else if( types[ i ] == 2002 || JDBCSupport.toTypeName( types[ i ] ) == null )
							ignore[ i ] = true;
					}

					if( parsed.coalesce != null )
						parsed.coalesce.bind( names );

					// Write header

					if( parsed.withHeader )
					{
						for( int i = 0; i < columns; i++ )
							if( !ignore[ i ] )
								csvWriter.writeValue( names[ i ] );
						csvWriter.nextRecord();
					}

					int count = 0;
					int progressNext = 1;
					while( result.next() )
					{
						Object[] values = new Object[ columns ];
						for( int i = 0; i < values.length; i++ )
							values[ i ] = JDBCSupport.getValue( result, types, i );

						if( parsed.coalesce != null )
							parsed.coalesce.coalesce( values );

						for( int i = 0; i < columns; i++ )
							if( !ignore[ i ] )
							{
								Object value = values[ i ];
								if( value == null )
									csvWriter.writeValue( (String)null );
								else if( value instanceof Clob )
								{
									Reader in = ( (Clob)value ).getCharacterStream();
									csvWriter.writeValue( in );
									in.close();
								}
								else if( value instanceof Blob )
								{
									InputStream in = ( (Blob)value ).getBinaryStream();
									csvWriter.writeValue( in );
									in.close();
								}
								else if( value instanceof byte[] )
									csvWriter.writeValue( (byte[])value );
								else
									csvWriter.writeValue( value.toString() );
							}

						csvWriter.nextRecord();

						count++;
						if( count >= progressNext )
						{
							progressNext = count + count / 10;
							processor.getProgressListener().println( "Written " + count + " records..." );
						}
//						if( count % 100000 == 0 )

						processor.getProgressListener().println( "Written " + count + " records." );
					}
				}
				finally
				{
					processor.closeStatement( statement, true );
				}
			}
			finally
			{
				csvWriter.close();
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

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "EXPORT" );
		tokenizer.get( "CSV" );

		Token t = tokenizer.get( "WITH", "SEPARATED", "COALESCE", "FILE" );
		if( t.eq( "WITH" ) )
		{
			tokenizer.get( "HEADER" );
			result.withHeader = true;

			t = tokenizer.get( "SEPARATED", "COALESCE", "FILE" );
		}

		if( t.eq( "SEPARATED" ) )
		{
			tokenizer.get( "BY" );
			t = tokenizer.get();
			if( t.eq( "TAB" ) )
				result.separator = '\t';
			else if( t.eq( "SPACE" ) )
				result.separator = ' ';
			else
			{
				if( t.length() != 1 )
					throw new SourceException( "Expecting [TAB], [SPACE] or a single character, not [" + t + "]", tokenizer.getLocation() );
				result.separator = t.getValue().charAt( 0 );
			}

			t = tokenizer.get( "COALESCE", "FILE" );
		}

		if( t.eq( "DATE" ) )
		{
			tokenizer.get( "AS" );
			tokenizer.get( "TIMESTAMP" );

			result.dateAsTimestamp = true;

			t = tokenizer.get();
		}

		while( t.eq( "COALESCE" ) )
		{
			if( result.coalesce == null )
				result.coalesce = new Coalescer();

			t = tokenizer.get();
			if( !t.isString() )
				throw new SourceException( "Expecting column name enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
			result.coalesce.first( t.stripQuotes() );

			t = tokenizer.get( "," );
			do
			{
				t = tokenizer.get();
				if( !t.isString() )
					throw new SourceException( "Expecting column name enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
				result.coalesce.next( t.stripQuotes() );

				t = tokenizer.get();
			}
			while( t.eq( "," ) );
			tokenizer.push( t );

			result.coalesce.end();

			tokenizer.get( "FILE" );
		}

		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new SourceException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get( "ENCODING" );
		t = tokenizer.get();
		String encoding = t.getValue();
		if( !encoding.startsWith( "\"" ) )
			throw new SourceException( "Expecting encoding enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		encoding = encoding.substring( 1, encoding.length() - 1 );

		t = tokenizer.get();
		if( t.eq( "GZIP" ) )
			result.gzip = true;
		else
			tokenizer.push( t );

		String query = tokenizer.getRemaining();

		result.fileName = file;
		result.encoding = encoding;
		result.query = query;

		return result;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}


	/**
	 * A parsed command.
	 *
	 * @author Ren� M. de Bloois
	 */
	static protected class Parsed
	{
		protected boolean withHeader;

		/** The separator. */
		protected char separator = ',';

		/** The file path to export to */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;

		protected boolean gzip;

		/** The query */
		protected String query;

		protected boolean dateAsTimestamp;

		/** Which columns need to be coalesced */
		protected Coalescer coalesce;
	}
}
