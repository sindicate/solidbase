/*--
 * Copyright 2006 Ren� M. de Bloois
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
import solidbase.core.SQLExecutionException;
import solidbase.core.SystemException;
import solidbase.util.Assert;
import solidbase.util.BOMDetectingLineReader;
import solidbase.util.JSONArray;
import solidbase.util.JSONObject;
import solidbase.util.JSONReader;
import solidbase.util.JSONParser.EVENT;
import solidbase.util.JdbcSupport;
import solidbase.util.LineReader;
import solidbase.util.Resource;
import solidbase.util.StringLineReader;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;


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
 * @author Ren� M. de Bloois
 * @since Dec 2, 2009
 */
// TODO Make this more strict, like assert that the number of values stays the same in the CSV data
public class ImportJSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "IMPORT\\s+JSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

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

		LineReader lineReader;
		if( parsed.reader != null )
			lineReader = parsed.reader; // Data is in the command
		else if( parsed.fileName != null )
		{
			// Data is in a file
			Resource resource = processor.getResource().createRelative( parsed.fileName );
			lineReader = new BOMDetectingLineReader( resource, "UTF-8" );
			// TODO What about the FileNotFoundException?
		}
		else
			lineReader = processor.getReader(); // Data is in the source file

		// Initialize csv reader & read first line (and skip header if needed)
		JSONReader reader = new JSONReader( lineReader );
		importNormal( command, processor, reader, parsed );
		return true;
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
	 * @throws SQLException Whenever SQL execution throws it.
	 */
	// TODO Cope with a variable number of values in the CSV list
	protected void importNormal( @SuppressWarnings( "unused" ) Command command, CommandProcessor processor, JSONReader reader, Parsed parsed ) throws SQLException
	{
		EVENT event = reader.next( EVENT.BEGIN_OBJECT );

		JSONObject properties = new JSONObject();
		event = reader.next();
		String name = reader.getName();
		while( !name.equals( "rows" ) )
		{
			Object value = reader.read();
			properties.set( name, value );

			reader.next();
			name = reader.getName();
		}

		reader.next( EVENT.BEGIN_ARRAY );

		JSONArray fields = properties.getArray( "fields" );
		int len = fields.size();
		int[] types = new int[ len ];
		for( int i = 0; i < len; i++ )
		{
			JSONObject field = (JSONObject)fields.get( i );
			types[ i ] = JdbcSupport.fromTypeName( field.getString( "type" ) );
		}

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
		boolean commit = false;
		try
		{
			int batchSize = 0;
			while( true )
			{
				if( Thread.currentThread().isInterrupted() )
					throw new ThreadDeath();

				event = reader.next();
				int lineNumber = reader.getLineNumber();
				if( event != EVENT.BEGIN_ARRAY )
					break;
				JSONArray values = reader.readArray();

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
					try
					{
						if( prependLineNumber )
						{
							if( par == 1 )
								statement.setInt( pos++, lineNumber );
							else
								statement.setObject( pos++, values.get( index = par - 2 ) );
						}
						else
							statement.setObject( pos++, values.get( index = par - 1 ) );
					}
					catch( ArrayIndexOutOfBoundsException e )
					{
						throw new CommandFileException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + values.size() + " values", reader.getLocation().lineNumber( lineNumber ) );
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
										b.append( values.get( par - 2 ) );
								}
								else
									b.append( values.get( par - 1 ) );
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
			}

			if( batchSize > 0 )
				statement.executeBatch();

			// TODO Read the rest of the file
			commit = true;
			return;
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

		Tokenizer tokenizer = new Tokenizer( new StringLineReader( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "IMPORT" );
		tokenizer.get( "JSV" );

		Token t = tokenizer.get( "PREPEND", "NOBATCH", "USING", "INTO" );

		if( t.equals( "PREPEND" ) )
		{
			tokenizer.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tokenizer.get( "NOBATCH", "USING", "INTO" );
		}

		if( t.equals( "NOBATCH" ) )
		{
			result.noBatch = true;

			t = tokenizer.get( "USING", "INTO" );
		}

		if( !t.equals( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tokenizer.getLocation() );
		result.tableName = tokenizer.get().toString();

		t = tokenizer.get( "(", "VALUES", "DATA", "FILE", null );

		if( t.equals( "(" ) )
		{
			t = tokenizer.get();
			if( t.equals( ")" ) || t.equals( "," ) )
				throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
			columns.add( t.getValue() );
			t = tokenizer.get( ",", ")" );
			while( !t.equals( ")" ) )
			{
				t = tokenizer.get();
				if( t.equals( ")" ) || t.equals( "," ) )
					throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
				columns.add( t.getValue() );
				t = tokenizer.get( ",", ")" );
			}

			t = tokenizer.get( "VALUES", "DATA", "FILE", null );
		}

		if( t.equals( "VALUES" ) )
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
			while( t.equals( "," ) );

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

		if( t.equals( "DATA" ) )
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
				if( t.equals( "(" ) )
				{
					//System.out.println( "(" );
					parseTill( tokenizer, result, true, ')' );
					t = tokenizer.get();
					Assert.isTrue( t.equals( ")" ) );
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
	 * @author Ren� M. de Bloois
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

		/** The underlying reader from the {@link Tokenizer}. */
		protected LineReader reader;

		/** The file path to import from */
		protected String fileName;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
