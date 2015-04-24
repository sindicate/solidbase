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
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.FatalException;
import solidbase.core.SQLExecutionException;
import solidbase.core.SourceException;
import solidbase.core.SystemException;
import solidbase.core.plugins.ImportCSV.ParsedFile;
import solidbase.util.Assert;
import solidbase.util.CloseQueue;
import solidbase.util.Counter;
import solidbase.util.FixedCounter;
import solidbase.util.JDBCSupport;
import solidbase.util.JSONArray;
import solidbase.util.JSONObject;
import solidbase.util.JSONReader;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidbase.util.TimedCounter;
import solidstack.io.Resource;
import solidstack.io.SegmentedInputStream;
import solidstack.io.SegmentedReader;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;
import solidstack.lang.ThreadInterrupted;


public class ImportJSON implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*IMPORT\\s+JSON\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

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

		// Parse the command
		Parsed parsed = parse( command );

		SourceReader sourceReader;
		Resource resource;
		boolean needClose = false;
		if( parsed.file != null )
		{
			// Data is in a file
			resource = processor.getResource().resolve( parsed.file.fileName );
			resource.setGZip( parsed.file.gzip );
			try
			{
				sourceReader = SourceReaders.forResource( resource, "UTF-8" );
			}
			catch( FileNotFoundException e )
			{
				throw new FatalException( e.toString() );
			}
			needClose = true;
			// TODO What about the FileNotFoundException?
		}
		else
		{
			sourceReader = processor.getReader(); // Data is in the source file
			resource = sourceReader.getResource(); // TODO Don't need this variable
		}

		// Create a JSON reader
		JSONReader reader = new JSONReader( sourceReader );
		try
		{
			// Read the header
			JSONObject properties = (JSONObject)reader.read();

			// The default binary file
			String binaryFile = properties.findString( "binaryFile" );

			// The fields
			JSONArray fields = properties.getArray( "fields" );
			int fieldCount = fields.size();

			// Initialise the working arrays
			int[] types = new int[ fieldCount ];
			String[] fileNames = new String[ fieldCount ];
			SegmentedInputStream[] streams = new SegmentedInputStream[ fieldCount ];
			SegmentedReader[] textStreams = new SegmentedReader[ fieldCount ];

			for( int i = 0; i < fieldCount; i++ )
			{
				JSONObject field = (JSONObject)fields.get( i );
				types[ i ] = JDBCSupport.fromTypeName( field.getString( "type" ) );
				fileNames[ i ] = field.findString( "file" );
			}

			boolean prependLineNumber = parsed.prependLineNumber;

			String sql;
			List< Integer > parameterMap = new ArrayList< Integer >();

//			if( parsed.sql != null )
//			{
				sql = parsed.sql;
				sql = LoadJSON.translateArgument( sql, parameterMap );
//			}
			/*
			else
			{
				StringBuilder sql1 = new StringBuilder( "INSERT INTO " );
				sql1.append( parsed.tableName );
				if( parsed.columns != null )
				{
					sql1.append( " (" );
					for( int i = 0; i < parsed.columns.length; i++ )
					{
						if( i > 0 )
							sql1.append( ',' );
						sql1.append( parsed.columns[ i ] );
					}
					sql1.append( ')' );
				}
				if( parsed.values != null )
				{
					sql1.append( " VALUES (" );
					for( int i = 0; i < parsed.values.length; i++ )
					{
						if( i > 0 )
							sql1.append( "," );
						String value = parsed.values[ i ];
						value = translateArgument( value, parameterMap );
						sql1.append( value );
					}
					sql1.append( ')' );
				}
				else
				{
					int count = line.length;
					if( parsed.columns != null )
						count = parsed.columns.length;
					if( prependLineNumber )
						count++;
					int par = 1;
					sql1.append( " VALUES (?" );
					parameterMap.add( par++ );
					while( par <= count )
					{
						sql1.append( ",?" );
						parameterMap.add( par++ );
					}
					sql1.append( ')' );
				}
				sql = sql1.toString();
			}
			*/

			// Create the log counter
			Counter counter = null;
			if( parsed.logRecords > 0 )
				counter = new FixedCounter( parsed.logRecords );
			else if( parsed.logSeconds > 0 )
				counter = new TimedCounter( parsed.logSeconds );

			// Prepare the INSERT statement
			PreparedStatement statement = processor.prepareStatement( sql.toString() );

			// Queues that will remember the files we need to close
			CloseQueue outerCloser = new CloseQueue();
			CloseQueue closer = new CloseQueue();

			boolean commit = false; // boolean to see if we reached the end
			try
			{
				int batchSize = 0;
				while( true )
				{
					// Detect interruption
					if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
						throw new ThreadInterrupted();

					// Read a record
					JSONArray values = (JSONArray)reader.read();
					if( values == null )
					{
						// End of file, finalize things
						Assert.isTrue( reader.isEOF() );
						if( batchSize > 0 )
							statement.executeBatch();

						if( counter != null && counter.needFinal() )
							processor.getProgressListener().println( "Imported " + counter.total() + " records." );

						commit = true;
						return true;
					}

					int lineNumber = reader.getLineNumber();

					// Convert the strings to date, time and timestamps
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

					// Set the statement parameters
					int pos = 1;
					for( int par : parameterMap )
					{
						if( par == 1 && prependLineNumber )
							statement.setInt( pos++, lineNumber );
						else
						{
							int index = par - ( prependLineNumber ? 2 : 1 );
							int type = types[ index ];
							Object value;
							try
							{
								value = values.get( index );
							}
							catch( ArrayIndexOutOfBoundsException e )
							{
								throw new SourceException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + values.size() + " values", reader.getLocation() );
							}
							if( value instanceof JSONObject )
							{
								// Value of parameter is in a separate file
								JSONObject object = (JSONObject)value;
								String filename = object.findString( "file" );
								if( filename != null )
								{
									// One file per record
									if( type == Types.BLOB || type == Types.VARBINARY )
									{
										try
										{
											// TODO Fix the input stream size given the size in the JSON file
											Resource r = resource.resolve( filename );
											BigDecimal filesize = object.findNumber( "size" );
											if( filesize == null || filesize.intValue() > 10240 ) // TODO Whats a good size here? Should it be a long?
											{
												// Some databases read the stream directly (Oracle), others read it later (HSQLDB).
												// TODO Do we need to decrease the batch size when files are being kept open?
												// TODO We could detect that the database has read the stream already, and close the file
												InputStream in = r.newInputStream();
												statement.setBinaryStream( pos++, in );
												closer.add( in );
											}
											else
												statement.setBytes( pos++, LoadJSON.readBytes( r ) ); // TODO Do a speed test
										}
										catch( FileNotFoundException e )
										{
											throw new SourceException( e.getMessage(), reader.getLocation() );
										}
									}
									else
										Assert.fail( "Unexpected field type for external file: " + JDBCSupport.toTypeName( type ) );
								}
								else
								{
									// One file for all records
									BigDecimal lobIndex = object.getNumber( "index" ); // TODO Use findNumber
									if( lobIndex == null )
										throw new SourceException( "Expected a 'file' or 'index' attribute", reader.getLocation() );
									BigDecimal lobLength = object.getNumber( "length" );
									if( lobLength == null )
										throw new SourceException( "Expected a 'length' attribute", reader.getLocation() );

									if( type == Types.BLOB || type == Types.VARBINARY )
									{
										// Get the input stream
										SegmentedInputStream in = streams[ index ];
										if( in == null )
										{
											// File not opened yet, open it
											String fileName = fileNames[ index ];
											if( fileName == null )
												fileName = binaryFile;
											if( fileName == null )
												throw new SourceException( "No file or default binary file configured", reader.getLocation() );
											Resource r = resource.resolve( fileName );
											try
											{
												in = new SegmentedInputStream( r.newInputStream() );
												outerCloser.add( in ); // Close at the final end
												streams[ index ] = in;
											}
											catch( FileNotFoundException e )
											{
												throw new SourceException( e.getMessage(), reader.getLocation() );
											}
										}
										statement.setBinaryStream( pos++, in.getSegmentInputStream( lobIndex.longValue(), lobLength.longValue() ) ); // TODO Maybe use the limited setBinaryStream instead
									}
									else if( type == Types.CLOB )
									{
										// Get the reader
										SegmentedReader in = textStreams[ index ];
										if( in == null )
										{
											// File not opened yet, open it
											if( fileNames[ index ] == null )
												throw new SourceException( "No file configured", reader.getLocation() );
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
												outerCloser.add( in ); // Close at the final end
												textStreams[ index ] = in;
											}
											catch( FileNotFoundException e )
											{
												throw new SourceException( e.getMessage(), reader.getLocation() );
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
//								if( type == Types.CLOB )
//								{
//									if( values.get( index ) == null )
//										System.out.println( "NULL!" );
//									else if( ( (String)values.get( index ) ).length() == 0 )
//										System.out.println( "EMPTY!" );
//
//									// TODO What if it is a CLOB and the string value is too long?
//									// Oracle needs this because CLOBs can contain empty strings "", and setObject() makes that null BUT THIS DOES NOT WORK!
//									statement.setCharacterStream( pos++, new StringReader( (String)values.get( index ) ) );
//								}
//								else
									// MonetDB complains when calling setObject with null value
//									Object v = values.get( index );
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
							String message = LoadJSON.buildErrorMessage( sql, parameterMap, values, prependLineNumber, lineNumber );
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

					if( counter != null && counter.next() )
						processor.getProgressListener().println( "Imported " + counter.total() + " records." );
				}
			}
			finally
			{
				processor.closeStatement( statement, commit );
				outerCloser.closeAll();
				closer.closeAll();
			}
		}
		finally
		{
			reader.close();
		}
	}


	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	static protected Parsed parse( Command command )
	{
		// FIXME Replace LINENUMBER with RECORD NUMBER
		/*
		IMPORT JSON
		[ PREPEND LINENUMBER ]
		[ NOBATCH ]
		[ LOG EVERY n RECORDS | SECONDS ]
		(
			[ FILE "<file>" ENCODING "<encoding>" [ GZIP ] ]
			EXECUTE ...
		)

		Later:

		|
			INTO <schema>.<table> [ ( <columns> ) ]
			[ VALUES ( <values> ) ]
			[ DATA | FILE ]
		*/

		Parsed result = new Parsed();
		List< String > columns = new ArrayList< String >();
		List< String > values = new ArrayList< String >();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "IMPORT" );
		tokenizer.get( "JSON" );

		Token t = tokenizer.get( "PREPEND", "NOBATCH", "LOG", "FILE", "EXECUTE" /*, "INTO" */ );

		if( t.eq( "PREPEND" ) )
		{
			tokenizer.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tokenizer.get( "NOBATCH", "LOG", "FILE", "EXECUTE" /*, "INTO" */ );
		}

		if( t.eq( "NOBATCH" ) )
		{
			result.noBatch = true;

			t = tokenizer.get( "LOG", "FILE", "EXECUTE" /*, "INTO" */ );
		}

		if( t.eq( "LOG" ) )
		{
			tokenizer.get( "EVERY" );
			t = tokenizer.get();
			if( !t.isNumber() )
				throw new SourceException( "Expecting a number, not [" + t + "]", tokenizer.getLocation() );

			int interval = Integer.parseInt( t.getValue() );
			t = tokenizer.get( "RECORDS", "SECONDS" );
			if( t.eq( "RECORDS" ) )
				result.logRecords = interval;
			else
				result.logSeconds = interval;

			t = tokenizer.get( "FILE", "EXECUTE" /*, "INTO" */ );
		}

		if( t.eq( "FILE" ) )
		{
			result.file = ImportCSV.parseFile( tokenizer );

			t = tokenizer.get( "EXECUTE" );
		}

//		if( t.eq( "EXECUTE" ) )
//		{
			result.sql = tokenizer.getRemaining();
			return result;
//		}

		/*
		tokenizer.expect( t, "INTO" );
		result.tableName = tokenizer.get().toString();

		t = tokenizer.get( ".", "(", "VALUES", "DATA", "FILE", null );

		if( t.eq( "." ) )
		{
			// TODO This means spaces are allowed, do we want that or not?
			result.tableName = result.tableName + "." + tokenizer.get().toString();

			t = tokenizer.get( "(", "VALUES", "DATA", "FILE", null );
		}

		if( t.eq( "(" ) )
		{
			t = tokenizer.get();
			if( t.eq( ")" ) || t.eq( "," ) )
				throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
			columns.add( t.getValue() );
			t = tokenizer.get( ",", ")" );
			while( !t.eq( ")" ) )
			{
				t = tokenizer.get();
				if( t.eq( ")" ) || t.eq( "," ) )
					throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
				columns.add( t.getValue() );
				t = tokenizer.get( ",", ")" );
			}

			t = tokenizer.get( "VALUES", "DATA", "FILE", null );
		}

		if( t.eq( "VALUES" ) )
		{
			tokenizer.get( "(" );
			do
			{
				StringBuilder value = new StringBuilder();
				parseTill( tokenizer, value, false, ',', ')' );
				//System.out.println( "Value: " + value.toString() );
				values.add( value.toString() );

				t = tokenizer.get( ",", ")" );
			}
			while( t.eq( "," ) );

			if( columns.size() > 0 )
				if( columns.size() != values.size() )
					throw new SourceException( "Number of specified columns does not match number of given values", tokenizer.getLocation() );

			t = tokenizer.get( "DATA", "FILE", null );
		}

		if( columns.size() > 0 )
			result.columns = columns.toArray( new String[ columns.size() ] );
		if( values.size() > 0 )
			result.values = values.toArray( new String[ values.size() ] );

		if( t.isEndOfInput() )
			return result;

		if( t.eq( "DATA" ) )
		{
			tokenizer.getNewline();
			result.reader = tokenizer.getReader();
			return result;
		}

		result.file = ImportCSV.parseFile( tokenizer ); // TODO Indicate that encoding is not allowed here

		tokenizer.get( (String)null );
		return result;
		*/
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

		protected int logRecords;
		protected int logSeconds;

		protected String sql;

//		/** The table name to insert into. */
//		protected String tableName;
//
//		/** The columns to insert into. */
//		protected String[] columns;

		/** The values to insert. Use :1, :2, etc to replace with the values from the CSV list. */
		protected String[] values;

//		/** The underlying reader from the {@link SQLTokenizer}. */
//		protected SourceReader reader;

		/** A file */
		protected ParsedFile file;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
