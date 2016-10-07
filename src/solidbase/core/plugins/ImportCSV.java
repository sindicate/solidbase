/*--
 * Copyright 2006 René M. de Bloois
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.FatalException;
import solidbase.util.FixedIntervalLogCounter;
import solidbase.util.LogCounter;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidbase.util.TimeIntervalLogCounter;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;


/**
 * This plugin executes IMPORT CSV statements.
 *
 * @author René M. de Bloois
 */
// TODO Make this more strict, like assert that the number of values stays the same in the CSV data
public class ImportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*IMPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		Parsed parsed = parse( command );

		if( skip )
		{
			if( parsed.reader == null && parsed.fileName == null )
			{
				SourceReader reader = processor.getReader(); // Data is in the source file, need to skip it.
				String line = reader.readLine();
				while( line != null && line.length() > 0 )
					line = reader.readLine();
			}
			return true;
		}

		// TODO BufferedInputStreams?
		SourceReader sourceReader;
		boolean needClose = false;
		if( parsed.reader != null )
			sourceReader = parsed.reader; // Data is in the command
		else if( parsed.fileName != null )
		{
			// Data is in a file
			Resource resource = processor.getResource().resolve( parsed.fileName );
			resource.setGZip( parsed.gzip );
			try
			{
				sourceReader = SourceReaders.forResource( resource, parsed.encoding );
			}
			catch( FileNotFoundException e )
			{
				throw new FatalException( e.toString() );
			}
			needClose = true;
			// TODO What about the FileNotFoundException?
		}
		else
			sourceReader = processor.getReader(); // Data is in the source file
		try
		{
			LogCounter counter = null;
			if( parsed.logRecords > 0 )
				counter = new FixedIntervalLogCounter( parsed.logRecords );
			else if( parsed.logSeconds > 0 )
				counter = new TimeIntervalLogCounter( parsed.logSeconds );

			CSVDataReader reader = new CSVDataReader( sourceReader, parsed.skipHeader, parsed.separator, !parsed.noEscape, parsed.ignoreWhiteSpace, parsed.prependLineNumber, counter != null ? new ImportLogger( counter, processor.getProgressListener() ) : null );
			DBWriter writer = new DBWriter( parsed.sql, parsed.tableName, parsed.columns, parsed.values, parsed.batchSize, parsed.batchCommit, processor );
			reader.setSink( new DefaultToJDBCTransformer( writer ) );

			boolean commit = false;
			try
			{
				reader.process();
				commit = true;
			}
			finally
			{
				writer.close( commit );
			}
			return true;
		}
		finally
		{
			if( needClose )
				sourceReader.close();
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
		IMPORT CSV
		[ FILE "<file>" ENCODING "<encoding>" [ GZIP ] ]
		[ INTO [ <schema> . ] <table> [ ( <columns> ) ] [ VALUES ( <values> ) ] ]
		[ SKIP HEADER ]
		[ SEPARATED BY TAB | SPACE | <character> ]
		[ ESCAPE NONE ]
		[ IGNORE WHITESPACE ]
		[ PREPEND LINENUMBER ]
		[ NOBATCH ]
		[ BATCH SIZE <n> [ WITH COMMIT ] ]
		[ LOG EVERY n RECORDS | SECONDS ]
		[ EXEC <sqlstatement> ]
		[ DATA ]

		- One of INTO or EXEC is needed
		- Only one of FILE or DATA is allowed, with DATA the data is in the rest of the command
		- If FILE and DATA is missing, the data will be read inline
		*/

		Parsed result = new Parsed();
		List< String > columns = new ArrayList< String >();
		List< String > values = new ArrayList< String >();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		EnumSet<Tokens> expected = EnumSet.of( Tokens.SKIP, Tokens.SEPARATED, Tokens.ESCAPE, Tokens.IGNORE, Tokens.PREPEND, Tokens.NOBATCH, Tokens.BATCH, Tokens.LOG, Tokens.INTO, Tokens.FILE, Tokens.EXEC, Tokens.DATA, Tokens.EOF );

		Token t = tokenizer.skip( "IMPORT" ).skip( "CSV" ).get();
		for( ;; )
			switch( tokenizer.expect( t, expected ) )
			{
				case SKIP:
					t = tokenizer.skip( "HEADER" ).get();
					result.skipHeader = true;
					expected.remove( Tokens.SKIP );
					break;

				case SEPARATED:
					t = tokenizer.skip( "BY" ).get();
					if( t.eq( "TAB" ) )
						result.separator = '\t';
					else if( t.eq( "SPACE" ) )
						result.separator = ' ';
					else
					{
						if( t.length() != 1 )
							throw new SourceException( "Expecting [TAB], [SPACE] or a single character, not [" + t + "]", tokenizer.getLocation() );
						result.separator = t.value().charAt( 0 );
					}
					t = tokenizer.get();
					expected.remove( Tokens.SEPARATED );
					break;

				case ESCAPE:
					t = tokenizer.skip( "NONE" ).get();
					result.noEscape = true;
					expected.remove( Tokens.ESCAPE );
					break;

				case IGNORE:
					t = tokenizer.skip( "WHITESPACE" ).get();
					result.ignoreWhiteSpace = true;
					expected.remove( Tokens.IGNORE );
					break;

				case PREPEND:
					t = tokenizer.skip( "LINENUMBER" ).get();
					result.prependLineNumber = true;
					expected.remove( Tokens.PREPEND );
					break;

				case NOBATCH:
					t = tokenizer.get();
					result.batchSize = 0;
					expected.remove( Tokens.NOBATCH );
					expected.remove( Tokens.BATCH );
					break;

				case BATCH:
					// TODO Add to CBOR and CSV too
					result.batchSize = Integer.parseInt( tokenizer.skip( "SIZE" ).getNumber().value() );
					if( ( t = tokenizer.get() ).eq( "WITH" ) )
					{
						t = tokenizer.skip( "COMMIT" ).get();
						result.batchCommit = true;
					}
					expected.remove( Tokens.NOBATCH );
					expected.remove( Tokens.BATCH );
					break;

				case LOG:
					int interval = Integer.parseInt( tokenizer.skip( "EVERY" ).getNumber().value() );
					if( tokenizer.get( "RECORDS", "SECONDS" ).eq( "RECORDS" ) )
						result.logRecords = interval;
					else
						result.logSeconds = interval;
					t = tokenizer.get();
					expected.remove( Tokens.LOG );
					break;

				case INTO:
					// TODO These tokens should also be added to the expected errors when not encountered
					result.tableName = tokenizer.getIdentifier().value();
					t = tokenizer.get();
					if( t.eq( "." ) )
					{
						result.tableName += "." + tokenizer.getIdentifier().value();
						t = tokenizer.get();
					}
					if( t.eq( "(" ) )
					{
						columns.add( tokenizer.getIdentifier().value() );
						t = tokenizer.get( ",", ")" );
						while( !t.eq( ")" ) )
						{
							columns.add( tokenizer.getIdentifier().value() );
							t = tokenizer.get( ",", ")" );
						}
						t = tokenizer.get();
					}
					if( t.eq( "VALUES" ) )
					{
						tokenizer.get( "(" );
						do
						{
							StringBuilder value = new StringBuilder();
							ImportJSON.parseTill( tokenizer, value, false, ',', ')' );
							values.add( value.toString() );
							t = tokenizer.get( ",", ")" );
						}
						while( t.eq( "," ) );
						if( columns.size() > 0 )
							if( columns.size() != values.size() )
								throw new SourceException( "Number of specified columns does not match number of given values", tokenizer.getLocation() );
						t = tokenizer.get();
					}
					if( columns.size() > 0 )
						result.columns = columns.toArray( new String[ columns.size() ] );
					if( values.size() > 0 )
						result.values = values.toArray( new String[ values.size() ] );
					expected.remove( Tokens.INTO );
					expected.remove( Tokens.EXEC );
					break;

				case FILE:
					result.fileName = tokenizer.getString().stripQuotes();
					result.encoding = tokenizer.skip( "ENCODING" ).getString().stripQuotes();
					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.gzip = true;
						t = tokenizer.get();
					}
					expected.remove( Tokens.FILE );
					expected.remove( Tokens.DATA );
					break;

				case EXEC:
					result.sql = tokenizer.getRemaining();
					return result;

				case DATA:
					tokenizer.getNewline();
					result.reader = tokenizer.getReader();
					//$FALL-THROUGH$

				case EOF:
					if( expected.contains( Tokens.INTO ) && expected.contains( Tokens.EXEC ) )
						throw new SourceException( "Missing INTO or EXEC", tokenizer.getLocation() );
					return result;

				default:
					throw new FatalException( "Unexpected token: " + t );
			}
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

		/** The separator. */
		protected char separator = ',';

		protected boolean noEscape;

		/** Ignore white space, except white space enclosed in double quotes. */
		protected boolean ignoreWhiteSpace;

		/** Prepend the values from the CSV list with the line number from the command file. */
		protected boolean prependLineNumber;

		protected int batchSize; // 0 is no batch
		protected boolean batchCommit;

		protected int logRecords;
		protected int logSeconds;

		protected String sql;

		/** The table name to insert into. */
		protected String tableName;

		/** The columns to insert into. */
		protected String[] columns;

		/** The values to insert. Use :1, :2, etc to replace with the values from the CSV list. */
		protected String[] values;

		/** The underlying reader from the {@link SQLTokenizer}. */
		protected SourceReader reader;

		/** The file path to import from */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;

		protected boolean gzip;
	}


	//@Override
	@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
