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

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Assert;
import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.SystemException;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;

import com.mindprod.csv.CSVReader;


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
public class ImportCSV extends CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "IMPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	static private final Pattern parameterPattern = Pattern.compile( ":{1,2}(\\d)+" );

//	static private final String syntax = "IMPORT CSV [SEPARATED BY TAB|<char>] [PREPEND LINENUMBER] [USING PLBLOCK|VALUESLIST] INTO <table> [(<colums>)] [VALUES (<values>)] DATA <newline> <data>";


	@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		Parsed parsed = parse( command );

		try
		{
			// Initialize csv reader & read first line
			CSVReader reader = new CSVReader( parsed.reader, parsed.separator, '"', "#", true, false, true );
			String[] line;
			try
			{
				line = reader.getAllFieldsInLine();
			}
			catch( EOFException e )
			{
				return true; // Nothing to insert and nothing to clean up
			}

			// Get connection and initialize commit flag
			Connection connection = processor.getCurrentDatabase().getConnection();
			Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
			boolean commit = false;
			try
			{
				if( parsed.usePLBlock )
					importUsingPLBlock( command, connection, reader, parsed, line );
				else if( parsed.useValuesList )
					importUsingValuesList( command, connection, reader, parsed, line );
				else
					importNormal( command, connection, reader, parsed, line );

				commit = true;
			}
			finally
			{
				if( commit )
					connection.commit();
				else
					connection.rollback();
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		return true;
	}


	/**
	 * Import data using a PL/SQL BEGIN/END block, like this:
	 * 
	 * <blockquote><pre>
	 * BEGIN
	 *     INSERT INTO TABLE1 VALUES ( VALUE1, VALUE2 );
	 *     INSERT INTO TABLE1 VALUES ( VALUE1, VALUE2 );
	 *     INSERT INTO TABLE1 VALUES ( VALUE1, VALUE2 );
	 * END;
	 * </pre></blockquote>
	 * 
	 * @param command The import command.
	 * @param connection The connection with the database.
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	protected void importUsingPLBlock( Command command, Connection connection, CSVReader reader, Parsed parsed, String[] line ) throws SQLException, IOException
	{
		String sql = generateSQLUsingPLBlock( reader, parsed, line );
		command.setCommand( sql );
		PreparedStatement statement = connection.prepareStatement( sql );
		try
		{
			statement.executeUpdate();
		}
		finally
		{
			statement.close();
		}
	}


	/**
	 * Generates the SQL for {@link #importUsingPLBlock(Command, Connection, CSVReader, Parsed, String[])}.
	 * 
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @return The generated SQL.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	// TODO Cope with variable number of values in the CSV list
	static protected String generateSQLUsingPLBlock( CSVReader reader, Parsed parsed, String[] line ) throws IOException
	{
		String tableName = parsed.tableName;
		String[] columns = parsed.columns;
		String[] values = parsed.values;
		boolean prependLineNumber = parsed.prependLineNumber;
		int lineNumber = parsed.lineNumber;

		StringBuilder sql = new StringBuilder();
		sql.append( "BEGIN\n" );
		while( line != null )
		{
			preprocess( line );

			sql.append( "INSERT INTO " );
			sql.append( tableName );
			if( columns != null )
			{
				sql.append( " (" );
				for( int i = 0; i < columns.length; i++ )
				{
					if( i > 0 )
						sql.append( ',' );
					sql.append( columns[ i ] );
				}
				sql.append( ')' );
			}
			sql.append( " VALUES (" );
			if( values != null )
			{
				for( int i = 0; i < values.length; i++ )
				{
					if( i > 0 )
						sql.append( "," );
					String value = values[ i ];
					value = translateArgument( value, prependLineNumber, lineNumber, line );
					sql.append( value );
				}
			}
			else
			{
				if( prependLineNumber )
				{
					sql.append( lineNumber );
					sql.append( ',' );
				}
				for( int i = 0; i < line.length; i++ )
				{
					if( i > 0 )
						sql.append( ',' );
					sql.append( quoteOrNull( line[ i ] ) );
				}
			}
			sql.append( ");\n" );

			try
			{
				line = reader.getAllFieldsInLine();
			}
			catch( EOFException e )
			{
				line = null;
			}
			lineNumber++; // TODO linenumber is useless this way
		}
		sql.append( "END;\n" );
		return sql.toString();
	}


	/**
	 * Import data using a ANSI SQL values list, like this:
	 * 
	 * <blockquote><pre>
	 * INSERT INTO TABLE1 VALUES
	 * ( VALUE1, VALUE2 ),
	 * ( VALUE1, VALUE2 ),
	 * ( VALUE1, VALUE2 );
	 * </pre></blockquote>
	 * 
	 * @param command The import command.
	 * @param connection The connection with the database.
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	protected void importUsingValuesList( Command command, Connection connection, CSVReader reader, Parsed parsed, String[] line ) throws SQLException, IOException
	{
		String sql = generateSQLUsingValuesList( reader, parsed, line );
		command.setCommand( sql );
		PreparedStatement statement = connection.prepareStatement( sql );
		try
		{
			statement.executeUpdate();
		}
		finally
		{
			statement.close();
		}
	}


	/**
	 * Generates the SQL for {@link #importUsingValuesList(Command, Connection, CSVReader, Parsed, String[])}.
	 * 
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @return The generated SQL.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	static protected String generateSQLUsingValuesList( CSVReader reader, Parsed parsed, String[] line ) throws IOException
	{
		String[] columns = parsed.columns;
		String[] values = parsed.values;
		boolean prependLineNumber = parsed.prependLineNumber;
		int lineNumber = parsed.lineNumber;

		StringBuilder sql = new StringBuilder();
		sql.append( "INSERT INTO " );
		sql.append( parsed.tableName );
		if( columns != null )
		{
			sql.append( " (" );
			sql.append( columns[ 0 ] );
			for( int i = 1; i < columns.length; i++ )
			{
				sql.append( ',' );
				sql.append( columns[ i ] );
			}
			sql.append( ')' );
		}
		sql.append( " VALUES " );
		while( line != null )
		{
			preprocess( line );

			if( columns != null && values == null )
				if( columns.length != ( prependLineNumber ? line.length + 1 : line.length ) )
					throw new CommandFileException( "Number of values does not match number of specified columns", lineNumber );

			sql.append( '(' );
			if( values != null )
			{
				for( int i = 0; i < values.length; i++ )
				{
					if( i > 0 )
						sql.append( "," );
					String value = values[ i ];
					value = translateArgument( value, prependLineNumber, lineNumber, line );
					sql.append( value );
				}
			}
			else
			{
				if( prependLineNumber )
				{
					sql.append( lineNumber );
					sql.append( ',' );
				}
				for( int i = 0; i < line.length; i++ )
				{
					if( i > 0 )
						sql.append( ',' );
					sql.append( quoteOrNull( line[ i ] ) );
				}
			}
			sql.append( ")" );

			try
			{
				line = reader.getAllFieldsInLine();
			}
			catch( EOFException e )
			{
				line = null;
			}

			if( line != null )
				sql.append( ',' );
			sql.append( '\n' );

			lineNumber++; // TODO linenumber is useless this way
		}
		return sql.toString();
	}


	/**
	 * Import data using a JDBC prepared statement, like this:
	 * 
	 * <blockquote><pre>
	 * INSERT INTO TABLE1 VALUES ( ?, ? );
	 * </pre></blockquote>
	 * 
	 * @param command The import command.
	 * @param connection The connection with the database.
	 * @param reader The CSV reader.
	 * @param parsed The parsed command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	// TODO Cope with variable number of values in the CSV list
	protected void importNormal( @SuppressWarnings( "unused" ) Command command, Connection connection, CSVReader reader, Parsed parsed, String[] line ) throws SQLException, IOException
	{
		boolean prependLineNumber = parsed.prependLineNumber;
		int lineNumber = parsed.lineNumber;

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
		List< Integer > parameterMap = new ArrayList();
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

		PreparedStatement statement = connection.prepareStatement( sql.toString() );

		try
		{
			while( true )
			{
				preprocess( line );
				statement.clearParameters();

				int pos = 1;
				for( int par : parameterMap )
				{
					if( prependLineNumber )
					{
						if( par == 1 )
							statement.setInt( pos++, lineNumber );
						else
							statement.setString( pos++, line[ par - 2 ] );
					}
					else
						statement.setString( pos++, line[ par - 1 ] );
				}
				//System.out.println( statement.toString() );
				statement.executeUpdate();

				try
				{
					line = reader.getAllFieldsInLine();
				}
				catch( EOFException e )
				{
					return;
				}
				lineNumber++;
			}
		}
		finally
		{
			statement.close();
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
	 * Replaces arguments within the given value with values from the CSV line.
	 * 
	 * @param value Value to be translated.
	 * @param prependLineNumber Is the line number added to the front of the CSV values?
	 * @param lineNumber The current line number.
	 * @param line Line of CSV values.
	 * @return The translated value.
	 */
	static protected String translateArgument( String value, boolean prependLineNumber, int lineNumber, String[] line )
	{
		Matcher matcher = parameterPattern.matcher( value );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int num = Integer.parseInt( matcher.group( 1 ) );
			if( prependLineNumber && num == 1 )
				matcher.appendReplacement( result, String.valueOf( lineNumber ) );
			else
			{
				if( prependLineNumber )
					num--;
				String field = line[ num - 1 ];
				if( !matcher.group().startsWith( "::" ) )
					field = quoteOrNull( field );
				else
					if( field == null )
						field = "NULL";
				matcher.appendReplacement( result, field );
			}
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
	 * Adds quotes, escapes, and return NULL if the value == null.
	 * 
	 * @param value The value to escape.
	 * @return Quoted value.
	 */
	static protected String quoteOrNull( String value )
	{
		if( value == null )
			return "NULL";
		return "'" + value.replaceAll( "'", "''" ) + "'";
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

		Tokenizer tokenizer = new Tokenizer( new StringReader( command.getCommand() ), command.getLineNumber() );

		tokenizer.get( "IMPORT" );
		tokenizer.get( "CSV" );

		Token t = tokenizer.get( "SEPARATED", "PREPEND", "USING", "INTO" );

		if( t.equals( "SEPARATED" ) )
		{
			tokenizer.get( "BY" );
			t = tokenizer.get();
			if( t.equals( "TAB" ) )
				result.separator = '\t';
			else
			{
				if( t.length() != 1 )
					throw new CommandFileException( "Expecting [TAB] or one character, not [" + t + "]", tokenizer.getLineNumber() );
				result.separator = t.getValue().charAt( 0 );
			}

			t = tokenizer.get( "PREPEND", "USING", "INTO" );
		}

		if( t.equals( "PREPEND" ) )
		{
			tokenizer.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tokenizer.get( "USING", "INTO" );
		}

		if( t.equals( "USING" ) )
		{
			t = tokenizer.get( "PLBLOCK", "VALUESLIST" );
			if( t.equals( "PLBLOCK" ) )
				result.usePLBlock = true;
			else
				result.useValuesList = true;

			t = tokenizer.get( "INTO" );
		}

		if( !t.equals( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tokenizer.getLineNumber() );
		result.tableName = tokenizer.get().toString();

		t = tokenizer.get( "(", "VALUES", "DATA" );

		if( t.equals( "(" ) )
		{
			t = tokenizer.get();
			if( t.equals( ")" ) || t.equals( "," ) )
				throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLineNumber() );
			columns.add( t.getValue() );
			t = tokenizer.get( ",", ")" );
			while( !t.equals( ")" ) )
			{
				t = tokenizer.get();
				if( t.equals( ")" ) || t.equals( "," ) )
					throw new CommandFileException( "Expecting a column name, not [" + t + "]", tokenizer.getLineNumber() );
				columns.add( t.getValue() );
				t = tokenizer.get( ",", ")" );
			}

			t = tokenizer.get( "VALUES", "DATA" );
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
					throw new CommandFileException( "Number of specified columns does not match number of given values", tokenizer.getLineNumber() );

			t = tokenizer.get( "DATA" );
		}

		if( !t.equals( "DATA" ) )
			throw new CommandFileException( "Expecting [DATA], not [" + t + "]", tokenizer.getLineNumber() );
		tokenizer.getNewline();

		result.lineNumber = tokenizer.getLineNumber();
		result.reader = tokenizer.getReader();

		if( columns.size() > 0 )
			result.columns = columns.toArray( new String[ columns.size() ] );
		if( values.size() > 0 )
			result.values = values.toArray( new String[ values.size() ] );

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
			throw new CommandFileException( "Unexpected EOF", tokenizer.getLineNumber() );
		if( t.length() == 1 )
			for( char c : chars )
				if( t.getValue().charAt( 0 ) == c )
					throw new CommandFileException( "Unexpected [" + t + "]", tokenizer.getLineNumber() );

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
					throw new CommandFileException( "Unexpected EOF", tokenizer.getLineNumber() );
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
		/**
		 * The separator.
		 */
		protected char separator = ',';
		/**
		 * Prepend the values from the CSV list with the line number from the command file.
		 */
		protected boolean prependLineNumber;
		/**
		 * The current line number in the command file.
		 */
		protected int lineNumber;
		/**
		 * Generate SQL with the ANSI SQL values list.
		 */
		protected boolean useValuesList;
		/**
		 * Generate SQL with the Oracle BEGIN END block.
		 */
		protected boolean usePLBlock;
		/**
		 * The table name to insert into.
		 */
		protected String tableName;
		/**
		 * The columns to insert into.
		 */
		protected String[] columns;
		/**
		 * The values to insert. Use :1, :2, etc to replace with the values from the CSV list.
		 */
		protected String[] values;
		/**
		 * The underlying reader from the {@link Tokenizer}.
		 */
		protected Reader reader;
	}
}
