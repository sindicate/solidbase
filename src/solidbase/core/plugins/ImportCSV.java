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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import solidbase.util.CSVReader;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;
import solidstack.io.Resource;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;


/**
 * This plugin executes IMPORT CSV statements.
 *
 * <blockquote><pre>
 * IMPORT CSV INTO tablename
 * "xxxx1","yyyy1","zzzz1"
 * "xxxx2","yyyy2","zzzz2"
 * GO
 * </pre></blockquote>
 *
 * @author René M. de Bloois
 * @since Dec 2, 2009
 */
// TODO Make this more strict, like assert that the number of values stays the same in the CSV data
public class ImportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "IMPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	static private final Pattern parameterPattern = Pattern.compile( ":(\\d+)" );


	//@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		Parsed parsed = parse( command );

		SourceReader lineReader;
		boolean needClose = false;
		if( parsed.reader != null )
			lineReader = parsed.reader; // Data is in the command
		else if( parsed.fileName != null )
		{
			// Data is in a file
			Resource resource = processor.getResource().resolve( parsed.fileName );
			try
			{
				lineReader = SourceReaders.forResource( resource, parsed.encoding );
			}
			catch( FileNotFoundException e )
			{
				throw new FatalException( e.toString() );
			}
			needClose = true;
			// TODO What about the FileNotFoundException?
		}
		else
			lineReader = processor.getReader(); // Data is in the source file

		try
		{
			// Initialize csv reader & read first line (and skip header if needed)
			CSVReader reader = new CSVReader( lineReader, parsed.separator, parsed.ignoreWhiteSpace );
			if( parsed.skipHeader )
			{
				String[] line = reader.getLine();
				if( line == null )
					return true;
			}
			int lineNumber = reader.getLineNumber();
			String[] line = reader.getLine();
			if( line == null )
				return true;

			importNormal( command, processor, reader, parsed, line, lineNumber );
			return true;
		}
		finally
		{
			if( needClose )
				lineReader.close();
		}
	}


	/**
	 * Import data using a JDBC prepared statement, like this:
	 *
	 * <blockquote><pre>
	 * INSERT INTO TABLE1 VALUES ( ?, ? );
	 * </pre></blockquote>
	 *
	 * @param command The import command.
	 * @param processor The command processor.
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @param lineNumber The current line number.
	 * @throws SQLException Whenever SQL execution throws it.
	 */
	// TODO Cope with a variable number of values in the CSV list
	protected void importNormal( @SuppressWarnings( "unused" ) Command command, CommandProcessor processor, CSVReader reader, Parsed parsed, String[] line, int lineNumber ) throws SQLException
	{
		boolean prependLineNumber = parsed.prependLineNumber;

		StringBuilder sql = new StringBuilder( "INSERT INTO " );
		sql.append( parsed.tableName );
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
			int count = line.length;
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
		boolean commit = false;
		try
		{
			int batchSize = 0;
//			int count = 0;
			while( true )
			{
				if( Thread.currentThread().isInterrupted() )
					throw new ThreadDeath();

				preprocess( line );

				int pos = 1;
				int index = 0;
				for( int par : parameterMap )
				{
					try
					{
						if( prependLineNumber )
						{
							if( par == 1 )
								statement.setInt( pos++, lineNumber );
							else
								statement.setString( pos++, line[ index = par - 2 ] );
						}
						else
							statement.setString( pos++, line[ index = par - 1 ] );
					}
					catch( ArrayIndexOutOfBoundsException e )
					{
						throw new CommandFileException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + line.length + " values", reader.getLocation().lineNumber( lineNumber ) );
					}
				}

				if( parsed.noBatch )
				{
					try
					{
						statement.executeUpdate();
					}
					catch( SQLException e )
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
										b.append( line[ par - 2 ] );
								}
								else
									b.append( line[ par - 1 ] );
							}
							catch( ArrayIndexOutOfBoundsException ee )
							{
								throw new SystemException( ee );
							}
						}
						b.append( ')' );

						// When NOBATCH is on, you can see the actual insert statement and line number in the file where the SQLException occurred.
						throw new SQLExecutionException( b.toString(), reader.getLocation().lineNumber( lineNumber ), e );
					}
				}
				else
				{
					statement.addBatch();
					batchSize++;
					if( batchSize >= 1000 )
					{
						statement.executeBatch();
						batchSize = 0;
					}
				}

				lineNumber = reader.getLineNumber();
				line = reader.getLine();
				if( line == null )
				{
					if( batchSize > 0 )
						statement.executeBatch();

					commit = true;
					return;
				}

//				count++; TODO
//				if( count % 100000 == 0 )
//					processor.getCallBack().println( "Read " + count + " records" );
			}
		}
		finally
		{
			processor.closeStatement( statement, commit );
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


	/**
	 * Replaces empty strings with null.
	 *
	 * @param line The line to preprocess.
	 */
	static protected void preprocess( String[] line )
	{
		for( int i = 0; i < line.length; i++ )
			if( line[ i ].length() == 0 )
				line[ i ] = null;
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

		Tokenizer tokenizer = new Tokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "IMPORT" );
		tokenizer.get( "CSV" );

		Token t = tokenizer.get( "SKIP", "SEPARATED", "IGNORE", "PREPEND", "NOBATCH", "USING", "INTO" );

		if( t.eq( "SKIP" ) )
		{
			tokenizer.get( "HEADER" );
			result.skipHeader = true;

			t = tokenizer.get( "SEPARATED", "IGNORE", "PREPEND", "NOBATCH", "USING", "INTO" );
		}

		if( t.eq( "SEPARATED" ) )
		{
			tokenizer.get( "BY" );
			t = tokenizer.get();
			if( t.eq( "TAB" ) )
				result.separator = '\t';
			else
			{
				if( t.length() != 1 )
					throw new CommandFileException( "Expecting [TAB] or one character, not [" + t + "]", tokenizer.getLocation() );
				result.separator = t.getValue().charAt( 0 );
			}

			t = tokenizer.get( "IGNORE", "PREPEND", "NOBATCH", "USING", "INTO" );
		}

		if( t.eq( "IGNORE" ) )
		{
			tokenizer.get( "WHITESPACE" );
			result.ignoreWhiteSpace = true;

			t = tokenizer.get( "PREPEND", "NOBATCH", "USING", "INTO" );
		}

		if( t.eq( "PREPEND" ) )
		{
			tokenizer.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tokenizer.get( "NOBATCH", "USING", "INTO" );
		}

		if( t.eq( "NOBATCH" ) )
		{
			result.noBatch = true;

			t = tokenizer.get( "USING", "INTO" );
		}

		if( !t.eq( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tokenizer.getLocation() );
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
					throw new CommandFileException( "Number of specified columns does not match number of given values", tokenizer.getLocation() );

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

		// File
		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get( "ENCODING" );
		t = tokenizer.get();
		String encoding = t.getValue();
		if( !encoding.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting encoding enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		encoding = encoding.substring( 1, encoding.length() - 1 );

		tokenizer.get( (String)null );

		result.fileName = file;
		result.encoding = encoding;
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
	static protected void parseTill( Tokenizer tokenizer, StringBuilder result, boolean includeInitialWhiteSpace, char... chars )
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
		/** Skip the header line. */
		protected boolean skipHeader = false;

		/** The separator. */
		protected char separator = ',';

		/** Ignore white space, except white space enclosed in double quotes. */
		protected boolean ignoreWhiteSpace;

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

		/** The underlying reader from the {@link Tokenizer}. */
		protected SourceReader reader;

		/** The file path to import from */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
