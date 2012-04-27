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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.FatalException;
import solidbase.core.SQLExecutionException;
import solidbase.core.SystemException;
import solidbase.util.Assert;
import solidbase.util.CloseQueue;
import solidbase.util.JDBCSupport;
import solidbase.util.JSONArray;
import solidbase.util.JSONObject;
import solidbase.util.JSONReader;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidstack.io.Resource;
import solidstack.io.SegmentedInputStream;
import solidstack.io.SegmentedReader;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;
import solidstack.lang.ThreadInterrupted;


public class LoadJSON implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*LOAD\\s+JSON\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	static private final Pattern parameterPattern = Pattern.compile( ":(\\d+)" );


	//@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		if( skip )
			return true;

		Parsed parsed = parse( command );

		Resource resource = processor.getResource().resolve( parsed.fileName );
		SourceReader lineReader;
		try
		{
			// TODO Use the same charset detection as JSON does. Maybe introduce the UTF charset if the default does not become UTF.
			lineReader = SourceReaders.forResource( resource, "UTF-8" );
		}
		catch( FileNotFoundException e )
		{
			throw new FatalException( e.toString() );
		}
		// TODO Close the lineReader

		JSONReader reader = new JSONReader( lineReader );

		JSONObject properties = (JSONObject)reader.read();
		String binaryFile = properties.findString( "binaryFile" );

		// Fields
		JSONArray fields = properties.getArray( "fields" );
		int len = fields.size();
		int[] types = new int[ len ];
		String[] fileNames = new String[ len ];
		SegmentedInputStream[] streams = new SegmentedInputStream[ len ];
		SegmentedReader[] textStreams = new SegmentedReader[ len ];
		for( int i = 0; i < len; i++ )
		{
			JSONObject field = (JSONObject)fields.get( i );
			types[ i ] = JDBCSupport.fromTypeName( field.getString( "type" ) );
			fileNames[ i ] = field.findString( "file" );
		}

		boolean prependLineNumber = parsed.prependLineNumber;

		StringBuilder sql = new StringBuilder( "INSERT INTO " );
		sql.append( parsed.tableName ); // TODO Where else do we need the quotes?
		if( parsed.columns != null )
		{
			sql.append( " (" );
			for( int i = 0; i < parsed.columns.length; i++ )
			{
				if( i > 0 )
					sql.append( ',' );
				sql.append( parsed.columns[ i ] );
			}
			sql.append( ')' );
		}
		List< Integer > parameterMap = new ArrayList< Integer >();
		if( parsed.values != null )
		{
			sql.append( " VALUES (" );
			for( int i = 0; i < parsed.values.length; i++ )
			{
				if( i > 0 )
					sql.append( "," );
				String value = parsed.values[ i ];
				value = translateArgument( value, parameterMap );
				sql.append( value );
			}
			sql.append( ')' );
		}
		else
		{
			int count = types.length;
			if( parsed.columns != null )
				count = parsed.columns.length;
			if( prependLineNumber )
				count++;
			int par = 1;
			sql.append( " VALUES (?" );
			parameterMap.add( par++ );
			while( par <= count )
			{
				sql.append( ",?" );
				parameterMap.add( par++ );
			}
			sql.append( ')' );
		}

		PreparedStatement statement = processor.prepareStatement( sql.toString() );
		CloseQueue biggerCloser = new CloseQueue();
		CloseQueue closer = new CloseQueue();
		boolean commit = false;
		try
		{
			int batchSize = 0;
//			int count = 0;
			while( true )
			{
				if( Thread.currentThread().isInterrupted() )
					throw new ThreadInterrupted();

				JSONArray values = (JSONArray)reader.read();
				if( values == null )
				{
					Assert.isTrue( reader.isEOF() );
					if( batchSize > 0 )
						statement.executeBatch();
					commit = true;
					return true;
				}
				int lineNumber = reader.getLineNumber();

				int i = 0;
				for( ListIterator< Object > it = values.iterator(); it.hasNext(); )
				{
					Object value = it.next();
					if( value != null )
					{
						if( types[ i ] == Types.DATE )
							it.set( java.sql.Date.valueOf( (String)value ) );
						else if( types[ i ] == Types.TIMESTAMP )
							it.set( java.sql.Timestamp.valueOf( (String)value ) );
						else if( types[ i ] == Types.TIME )
							it.set( java.sql.Time.valueOf( (String)value ) );
					}
					i++;
				}

				int pos = 1;
				int index = 0;
				for( int par : parameterMap )
				{
					if( par == 1 && prependLineNumber )
						statement.setInt( pos++, lineNumber );
					else
					{
						index = par - ( prependLineNumber ? 2 : 1 );
						int type = types[ index ];
						Object value;
						try
						{
							value = values.get( index );
						}
						catch( ArrayIndexOutOfBoundsException e )
						{
							throw new CommandFileException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + values.size() + " values", reader.getLocation() );
						}
						if( value instanceof JSONObject )
						{
							JSONObject object = (JSONObject)value;
							String filename = object.findString( "file" );
							if( filename != null )
							{
								if( type == Types.BLOB || type == Types.VARBINARY )
								{
									try
									{
										InputStream in = resource.resolve( filename ).newInputStream();
										closer.add( in );
										// Some databases read the stream directly (Oracle), others read it later (HSQLDB).
										statement.setBinaryStream( pos++, in );
										// TODO Do we need to decrease the batch size when files are being kept open?
										// TODO What if the contents of the blob is sufficiently small, shouldn't we just call setBytes()?
										// TODO We could detect that the database has read the stream already, and close the file
									}
									catch( FileNotFoundException e )
									{
										throw new CommandFileException( e.getMessage(), reader.getLocation() );
									}
								}
								else
									Assert.fail( "Unexpected field type for external file: " + JDBCSupport.toTypeName( type ) );
							}
							else
							{
								BigDecimal lobIndex = object.getNumber( "index" ); // TODO Use findNumber
								if( lobIndex == null )
									throw new CommandFileException( "Expected a 'file' or 'index' attribute", reader.getLocation() );
								BigDecimal lobLength = object.getNumber( "length" );
								if( lobLength == null )
									throw new CommandFileException( "Expected a 'length' attribute", reader.getLocation() );

								if( type == Types.BLOB || type == Types.VARBINARY )
								{
									String fileName = fileNames[ index ];
									if( fileName == null )
										fileName = binaryFile;
									if( fileName == null )
										throw new CommandFileException( "No file configured", reader.getLocation() );
									SegmentedInputStream in = streams[ index ];
									if( in == null )
									{
										Resource r = resource.resolve( fileName );
										try
										{
											in = new SegmentedInputStream( r.newInputStream() );
											biggerCloser.add( in ); // TODO Why? Don't understand anymore.
											streams[ index ] = in;
										}
										catch( FileNotFoundException e )
										{
											throw new CommandFileException( e.getMessage(), reader.getLocation() );
										}
									}
									statement.setBinaryStream( pos++, in.getSegmentInputStream( lobIndex.longValue(), lobLength.longValue() ) ); // TODO Maybe use the limited setBinaryStream instead
								}
								else if( type == Types.CLOB )
								{
									if( fileNames[ index ] == null )
										throw new CommandFileException( "No file configured", reader.getLocation() );
									SegmentedReader in = textStreams[ index ];
									if( in == null )
									{
										Resource r = resource.resolve( fileNames[ index ] );
										try
										{
											try
											{
												in = new SegmentedReader( new InputStreamReader( r.newInputStream(), "UTF-8" ) );
											}
											catch( UnsupportedEncodingException e )
											{
												throw new SystemException( e );
											}
											biggerCloser.add( in );
											textStreams[ index ] = in;
										}
										catch( FileNotFoundException e )
										{
											throw new CommandFileException( e.getMessage(), reader.getLocation() );
										}
									}
									statement.setCharacterStream( pos++, in.getSegmentReader( lobIndex.longValue(), lobLength.longValue() ) );
								}
								else
									Assert.fail( "Unexpected field type for external file: " + JDBCSupport.toTypeName( type ) );
							}
						}
						else
						{
//							if( type == Types.CLOB )
//							{
//								if( values.get( index ) == null )
//									System.out.println( "NULL!" );
//								else if( ( (String)values.get( index ) ).length() == 0 )
//									System.out.println( "EMPTY!" );
//
//								// TODO What if it is a CLOB and the string value is too long?
//								// Oracle needs this because CLOBs can contain empty strings "", and setObject() makes that null BUT THIS DOES NOT WORK!
//								statement.setCharacterStream( pos++, new StringReader( (String)values.get( index ) ) );
//							}
//							else
								// MonetDB complains when calling setObject with null value
//								Object v = values.get( index );
//								if( v != null )
								statement.setObject( pos++, values.get( index ) );
//								else
//									statement.setNull( pos++, type );
						}
					}
				}

				if( parsed.noBatch )
				{
					try
					{
						statement.executeUpdate();
						closer.closeAll();
					}
					catch( SQLException e )
					{
						// When NOBATCH is on, you can see the actual insert statement and line number in the file where the SQLException occurred.
						String message = buildErrorMessage( sql, parameterMap, values, prependLineNumber, lineNumber );
						throw new SQLExecutionException( message, reader.getLocation().lineNumber( lineNumber ), e );
					}
				}
				else
				{
					statement.addBatch();
					batchSize++;
					// TODO Also check the closer's count
					if( batchSize >= 1000 )
					{
						statement.executeBatch();
						batchSize = 0;
						closer.closeAll();
					}
				}

//				count++; TODO
//				if( count % 100000 == 0 )
//					processor.getCallBack().println( "Read " + count + " records" );
			}
		}
		finally
		{
			processor.closeStatement( statement, commit );
			biggerCloser.closeAll();
			closer.closeAll();
		}
	}


	/**
	 * Replaces arguments within the given value with ? and maintains a map.
	 *
	 * @param value Value to be translated.
	 * @param parameterMap A map of ? index to index of the CSV fields.
	 * @return The translated value.
	 */
	static protected String translateArgument( String value, List< Integer > parameterMap )
	{
		Matcher matcher = parameterPattern.matcher( value );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int num = Integer.parseInt( matcher.group( 1 ) );
			parameterMap.add( num );
			matcher.appendReplacement( result, "?" );
		}
		matcher.appendTail( result );
		return result.toString();
	}


	static protected String buildErrorMessage( StringBuilder sql, List< Integer > parameterMap, JSONArray values, boolean prependLineNumber, int lineNumber )
	{
		StringBuilder b = new StringBuilder( sql.toString() );
		b.append( " VALUES (" );
		boolean first = true;
		for( int par : parameterMap )
		{
			if( first )
				first = false;
			else
				b.append( ',' );
			try
			{
				if( prependLineNumber )
				{
					if( par == 1 )
						b.append( lineNumber );
					else
						b.append( values.get( par - 2 ) );
				}
				else
					b.append( values.get( par - 1 ) );
			}
			catch( ArrayIndexOutOfBoundsException ee ) // TODO Why is this caught?
			{
				throw new SystemException( ee );
			}
		}
		b.append( ')' );
		return b.toString();
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
		List< String > columns = new ArrayList< String >();
		List< String > values = new ArrayList< String >();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "LOAD" );
		tokenizer.get( "JSON" );

		Token t = tokenizer.get( "PREPEND", "NOBATCH", "INTO" );

		if( t.eq( "PREPEND" ) )
		{
			tokenizer.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tokenizer.get( "NOBATCH", "INTO" );
		}

		if( t.eq( "NOBATCH" ) )
		{
			result.noBatch = true;

			t = tokenizer.get( "INTO" );
		}

		if( !t.eq( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tokenizer.getLocation() );
		result.tableName = tokenizer.get().toString();

		t = tokenizer.get( ".", "(", "VALUES", "FILE" );

		if( t.eq( "." ) )
		{
			// TODO This means spaces are allowed, do we want that or not?
			result.tableName = result.tableName + "." + tokenizer.get().toString();

			t = tokenizer.get( "(", "VALUES", "FILE" );
		}

		if( t.eq( "(" ) )
		{
			t = tokenizer.get();
			if( t.eq( ")" ) || t.eq( "," ) )
				throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
			columns.add( t.getValue() );
			t = tokenizer.get( ",", ")" );
			while( !t.eq( ")" ) )
			{
				t = tokenizer.get();
				if( t.eq( ")" ) || t.eq( "," ) )
					throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
				columns.add( t.getValue() );
				t = tokenizer.get( ",", ")" );
			}

			t = tokenizer.get( "VALUES", "FILE" );
		}

		if( t.eq( "VALUES" ) )
		{
			tokenizer.get( "(" );
			do
			{
				StringBuilder value = new StringBuilder();
				parseTill( tokenizer, value, false, ',', ')' );
				values.add( value.toString() );

				t = tokenizer.get( ",", ")" );
			}
			while( t.eq( "," ) );

			if( columns.size() > 0 )
				if( columns.size() != values.size() )
					throw new CommandFileException( "Number of specified columns does not match number of given values", tokenizer.getLocation() );

			t = tokenizer.get( "FILE" );
		}

		if( columns.size() > 0 )
			result.columns = columns.toArray( new String[ columns.size() ] );
		if( values.size() > 0 )
			result.values = values.toArray( new String[ values.size() ] );

		// File
		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		tokenizer.get( (String)null );

		result.fileName = file;
		return result;
	}


	/**
	 * Parse till the specified characters are found.
	 *
	 * @param tokenizer The tokenizer.
	 * @param result The result is stored in this StringBuilder.
	 * @param chars The end characters.
	 * @param includeInitialWhiteSpace Include the whitespace that precedes the first token.
	 */
	static protected void parseTill( SQLTokenizer tokenizer, StringBuilder result, boolean includeInitialWhiteSpace, char... chars )
	{
		Token t = tokenizer.get();
		if( t == null )
			throw new CommandFileException( "Unexpected EOF", tokenizer.getLocation() );
		if( t.length() == 1 )
			for( char c : chars )
				if( t.getValue().charAt( 0 ) == c )
					throw new CommandFileException( "Unexpected [" + t + "]", tokenizer.getLocation() );

		if( includeInitialWhiteSpace )
			result.append( t.getWhiteSpace() );
		result.append( t.getValue() );

		outer:
			while( true )
			{
				if( t.eq( "(" ) )
				{
					//System.out.println( "(" );
					parseTill( tokenizer, result, true, ')' );
					t = tokenizer.get();
					Assert.isTrue( t.eq( ")" ) );
					//System.out.println( ")" );
					result.append( t.getWhiteSpace() );
					result.append( t.getValue() );
				}

				t = tokenizer.get();
				if( t == null )
					throw new CommandFileException( "Unexpected EOF", tokenizer.getLocation() );
				if( t.length() == 1 )
					for( char c : chars )
						if( t.getValue().charAt( 0 ) == c )
							break outer;

				result.append( t.getWhiteSpace() );
				result.append( t.getValue() );
			}

		tokenizer.push( t );
	}


	/**
	 * A parsed command.
	 *
	 * @author René M. de Bloois
	 */
	static protected class Parsed
	{
		/** Prepend the values from the CSV list with the line number from the command file. */
		protected boolean prependLineNumber;

		/** Don't use JDBC batch update. */
		protected boolean noBatch;

		/** The table name to insert into. */
		protected String tableName;

		/** The columns to insert into. */
		protected String[] columns;

		/** The values to insert. Use :1, :2, etc to replace with the values from the CSV list. */
		protected String[] values;

//		/** The underlying reader from the {@link Tokenizer}. */
//		protected SourceReader reader;

		/** The file path to import from */
		protected String fileName;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
