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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
import solidstack.io.SourceInputStream;
import solidstack.io.SourceReaders;


public class LoadCBOR implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*LOAD\\s+CBOR\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

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

		// TODO BufferedInputStreams?
		// Open the file resource
		Resource resource = processor.getResource().resolve( parsed.fileName );
		resource.setGZip( parsed.gzip );
		SourceInputStream in;
		try
		{
			in = SourceReaders.forBinaryResource( resource );
		}
		catch( FileNotFoundException e )
		{
			throw new FatalException( e.toString() );
		}

		try
		{
			LogCounter counter = null;
			if( parsed.logRecords > 0 )
				counter = new FixedIntervalLogCounter( parsed.logRecords );
			else if( parsed.logSeconds > 0 )
				counter = new TimeIntervalLogCounter( parsed.logSeconds );

			CBORDataReader reader = new CBORDataReader( in, counter != null ? new ImportLogger( counter, processor.getProgressListener() ) : null );

			// TODO Test prependlinenumbers

			String[] columns;
			if( parsed.columns != null )
				columns = parsed.columns;
			else
			{
				columns = reader.getFieldNames();
				for( int i = 0; i < columns.length; i++ )
					if( StringUtils.isBlank( columns[ i ] ) )
						throw new FatalException( "Field name is blank for field number " + i + " in " + resource );
			}

			DBWriter writer = new DBWriter( null, parsed.tableName, parsed.columns, parsed.values, parsed.noBatch, processor );
			reader.setOutput( writer );

			boolean commit = false;
			try
			{
				reader.process();
				commit = true;
			}
			finally
			{
				writer.end( commit );
			}
			return true;
		}
		finally
		{
			in.close();
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
		// TODO Match column names
		// TODO Free SQL like with IMPORT CSV
		/*
		LOAD CBOR
		[ PREPEND LINENUMBER ]
		[ NOBATCH ]
		[ LOG EVERY n RECORDS | SECONDS ]
		INTO <schema>.<table> [ ( <columns> ) ]
		[ VALUES ( <values> ) ]
		FILE "<file>" [ GZIP ]
		*/

		Parsed result = new Parsed();
		List< String > columns = new ArrayList< String >();
		List< String > values = new ArrayList< String >();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "LOAD" );
		tokenizer.get( "CBOR" );

		Token t = tokenizer.get( "NOBATCH", "LOG", "INTO" );

		if( t.eq( "NOBATCH" ) )
		{
			result.noBatch = true;

			t = tokenizer.get( "LOG", "INTO" );
		}

		if( t.eq( "LOG" ) )
		{
			tokenizer.get( "EVERY" );
			t = tokenizer.get();
			if( !t.isNumber() )
				throw new SourceException( "Expecting a number, not [" + t + "]", tokenizer.getLocation() );

			int interval = Integer.parseInt( t.value() );
			t = tokenizer.get( "RECORDS", "SECONDS" );
			if( t.eq( "RECORDS" ) )
				result.logRecords = interval;
			else
				result.logSeconds = interval;

			t = tokenizer.get( "INTO" );
		}

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
				throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
			columns.add( t.value() );
			t = tokenizer.get( ",", ")" );
			while( !t.eq( ")" ) )
			{
				t = tokenizer.get();
				if( t.eq( ")" ) || t.eq( "," ) )
					throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
				columns.add( t.value() );
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
					throw new SourceException( "Number of specified columns does not match number of given values", tokenizer.getLocation() );

			t = tokenizer.get( "FILE" );
		}

		if( columns.size() > 0 )
			result.columns = columns.toArray( new String[ columns.size() ] );
		if( values.size() > 0 )
			result.values = values.toArray( new String[ values.size() ] );

		// File
		t = tokenizer.get();
		String file = t.value();
		if( !file.startsWith( "\"" ) )
			throw new SourceException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get();
		if( t.eq( "GZIP" ) )
		{
			result.gzip = true;
			t = tokenizer.get();
		}

		tokenizer.expect( t, (String)null );

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

		/** Don't use JDBC batch update. */
		protected boolean noBatch;

		protected int logRecords;
		protected int logSeconds;

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


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
