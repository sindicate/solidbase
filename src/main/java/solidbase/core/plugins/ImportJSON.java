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
import solidbase.util.Assert;
import solidbase.util.FixedIntervalLogCounter;
import solidbase.util.LogCounter;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidbase.util.TimeIntervalLogCounter;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;


public class ImportJSON implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*IMPORT\\s+JSON\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isAnnotation() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		Parsed parsed = parse( command );

		if( skip )
		{
			if( parsed.fileName == null )
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
		Resource resource = null;
		boolean inline = false;
		if( parsed.fileName != null )
		{
			// Data is in a file
			resource = processor.getResource().resolve( parsed.fileName );
			resource.setGZip( parsed.gzip );
			try
			{
				sourceReader = SourceReaders.forResource( resource, "UTF-8" );
			}
			catch( FileNotFoundException e )
			{
				throw new FatalException( e.toString() );
			}
			// TODO What about the FileNotFoundException?
		}
		else
		{
			sourceReader = processor.getReader(); // Data is in the source file
			inline = true;
		}
		try
		{
			LogCounter counter = null;
			if( parsed.logRecords > 0 )
				counter = new FixedIntervalLogCounter( parsed.logRecords );
			else if( parsed.logSeconds > 0 )
				counter = new TimeIntervalLogCounter( parsed.logSeconds );

			JSONDataReader reader = new JSONDataReader( sourceReader, parsed.prependLineNumber, inline, counter != null ? new ImportLogger( counter, processor.getProgressListener() ) : null );

			// TODO Test prependlinenumbers

			// FIXME This does not work
			/*
			String[] columns;
			if( parsed.columns != null )
				columns = parsed.columns;
			else
			{
				columns = reader.getFieldNames();
				for( int i = 0; i < columns.length; i++ )
					if( StringUtils.isBlank( columns[ i ] ) )
						throw new FatalException( "Field name is blank for field number " + i + ( resource != null ? " in " + resource : "" ) );
			}
			*/

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
			if( !inline )
				sourceReader.close();
		}
	}

	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	// TODO Need to support columns name with "" or with `` like in other databases
	static protected Parsed parse( Command command )
	{
		// FIXME Replace LINENUMBER with RECORDNUMBER
		// TODO Match column names

		/*
		IMPORT JSON
		[ FILE "<file>" [ GZIP ] ]
		[ INTO [ <schema> . ] <table> [ ( <columns> ) ] [ VALUES ( <values> ) ] ]
		[ PREPEND LINENUMBER ]
		[ NOBATCH ]
		[ BATCH SIZE <n> [ WITH COMMIT ] ]
		[ LOG EVERY <n> RECORDS | SECONDS ]
		[ EXEC <sqlstatement> ]

		- One of INTO or EXEC is needed
		- If FILE is missing, the data will be read inline
		*/

		Parsed result = new Parsed();
		List< String > columns = new ArrayList<>();
		List< String > values = new ArrayList<>();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		EnumSet<Tokens> expected = EnumSet.of( Tokens.PREPEND, Tokens.NOBATCH, Tokens.BATCH, Tokens.LOG, Tokens.INTO, Tokens.FILE, Tokens.EXEC, Tokens.EOF );

		Token t = tokenizer.skip( "IMPORT" ).skip( "JSON" ).get();
		for( ;; )
			switch( tokenizer.expect( t, expected ) )
			{
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
							parseTill( tokenizer, value, false, ',', ')' );
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
					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.gzip = true;
						t = tokenizer.get();
					}
					expected.remove( Tokens.FILE );
					break;

				case EXEC:
					result.sql = tokenizer.getRemaining();
					return result;

				case EOF:
					if( expected.contains( Tokens.INTO ) && expected.contains( Tokens.EXEC ) )
						throw new SourceException( "Missing INTO or EXEC", tokenizer.getLocation() );
					return result;

				default:
					throw new FatalException( "Unexpected token: " + t );
			}
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
			throw new SourceException( "Unexpected EOF", tokenizer.getLocation() );
		if( t.length() == 1 )
			for( char c : chars )
				if( t.value().charAt( 0 ) == c )
					throw new SourceException( "Unexpected [" + t + "]", tokenizer.getLocation() );

		if( includeInitialWhiteSpace )
			result.append( t.whiteSpace() );
		result.append( t.value() );

		outer:
			while( true )
			{
				// TODO What about quotes?
				if( t.eq( "(" ) )
				{
					//System.out.println( "(" );
					parseTill( tokenizer, result, true, ')' );
					t = tokenizer.get();
					Assert.isTrue( t.eq( ")" ) );
					//System.out.println( ")" );
					result.append( t.whiteSpace() );
					result.append( t.value() );
				}

				t = tokenizer.get();
				if( t == null )
					throw new SourceException( "Unexpected EOF", tokenizer.getLocation() );
				if( t.length() == 1 )
					for( char c : chars )
						if( t.value().charAt( 0 ) == c )
							break outer;

				result.append( t.whiteSpace() );
				result.append( t.value() );
			}

		tokenizer.rewind();
	}


	/**
	 * A parsed command.
	 *
	 * @author René M. de Bloois
	 */
	static protected class Parsed
	{
		/** Prepend the values from the CSV list with the line number from the command file. */
		protected boolean prependLineNumber; // TODO Remove, after it is made possible to use an expression for auto increment

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

//		/** The underlying reader from the {@link Tokenizer}. */
//		protected SourceReader reader;

		/** The file path to import from */
		protected String fileName;
		protected boolean gzip;
	}


	@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
