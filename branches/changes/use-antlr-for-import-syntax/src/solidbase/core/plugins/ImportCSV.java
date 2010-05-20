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
import solidbase.util.PushbackReader;

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
 * This plugin will transform the CSV lines into INSERT INTO tablename VALUES ("xxxx","yyyy","zzzz").
 *
 * @author René M. de Bloois
 * @since Dec 2, 2009
 */
// TODO Make this more strict, like assert that the number of values stays the same in the CSV data
public class ImportCSV extends CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "IMPORT\\s+CSV.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	static private final Pattern parameterPattern = Pattern.compile( ":(\\d)+" );

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
					importUsingValuesList( command, connection, reader, parsed.tableName, parsed.columns, parsed.prependLineNumber, parsed.lineNumber, line );
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
	static protected String generateSQLUsingPLBlock( CSVReader reader, Parsed parsed, String[] line ) throws IOException
	{
		String[] columns = parsed.columns;
		String[] values = parsed.values;

		StringBuilder sql = new StringBuilder();
		sql.append( "BEGIN\n" );
		while( line != null )
		{
			sql.append( "INSERT INTO " );
			sql.append( parsed.tableName );
			if( parsed.columns != null )
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
			if( values != null )
			{
				sql.append( " VALUES (" );
				for( int i = 0; i < values.length; i++ )
				{
					if( i > 0 )
						sql.append( "," );
					String value = values[ i ];
					value = translateArgument( value, parsed.prependLineNumber, parsed.lineNumber, line );
					sql.append( value );
				}
			}
			else
			{
				sql.append( " VALUES (" );
				if( parsed.prependLineNumber )
				{
					sql.append( parsed.lineNumber );
					sql.append( ',' );
				}
				for( int i = 0; i < line.length; i++ )
				{
					if( i > 0 )
						sql.append( ',' );
					sql.append( '\'' );
					sql.append( escape( line[ i ] ) );
					sql.append( '\'' );
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
			parsed.lineNumber++;
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
	 * @param tableName The table name to import into.
	 * @param columns The optional column list.
	 * @param prependLineNumber Use the line number as the first value.
	 * @param lineNumber The line number of the import command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	protected void importUsingValuesList( Command command, Connection connection, CSVReader reader, String tableName, String[] columns, boolean prependLineNumber, int lineNumber, String[] line ) throws SQLException, IOException
	{
		StringBuilder sql = new StringBuilder();
		sql.append( "INSERT INTO " );
		sql.append( tableName );
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
		sql.append( " VALUES\n" );
		boolean comma = false;
		while( line != null )
		{
			if( columns != null )
				if( columns.length != ( prependLineNumber ? line.length + 1 : line.length ) )
					throw new CommandFileException( "Number of specified columns does not match the number of values in a line of data", command.getLineNumber() );

			if( comma )
				sql.append( ',' );
			sql.append( '(' );
			if( prependLineNumber )
			{
				sql.append( lineNumber );
				sql.append( ',' );
			}
			for( int i = 0; i < line.length; i++ )
			{
				if( i > 0 )
					sql.append( ',' );
				sql.append( '\'' );
				sql.append( escape( line[ i ] ) );
				sql.append( '\'' );
			}
			sql.append( ")\n" );
			try
			{
				line = reader.getAllFieldsInLine();
			}
			catch( EOFException e )
			{
				line = null;
			}
			comma = true;
			lineNumber++;
		}
		sql.append( '\n' );
		String s = sql.toString();
		command.setCommand( s );
		PreparedStatement statement = connection.prepareStatement( sql.toString() );
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
	protected void importNormal( @SuppressWarnings( "unused" ) Command command, Connection connection, CSVReader reader, Parsed parsed, String[] line ) throws SQLException, IOException
	{
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
			if( parsed.prependLineNumber )
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
				statement.clearParameters();

				int pos = 1;
				for( int par : parameterMap )
				{
					if( parsed.prependLineNumber )
					{
						if( par == 1 )
							statement.setInt( pos++, parsed.lineNumber );
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
				parsed.lineNumber++;
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
			if( prependLineNumber )
			{
				if( num == 1 )
					matcher.appendReplacement( result, String.valueOf( lineNumber ) );
				else
					matcher.appendReplacement( result, "'" + line[ num - 2 ] + "'" );
			}
			else
				matcher.appendReplacement( result, "'" + line[ num - 1 ] + "'" );
		}
		matcher.appendTail( result );
		return result.toString();
	}


	/**
	 * Converts ' to ''.
	 * 
	 * @param s The sql to escape.
	 * @return Escaped sql.
	 */
	static protected String escape( String s )
	{
		return s.replaceAll( "'", "''" );
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

		Tokenizer tr = new Tokenizer( new StringReader( command.getCommand() ), command.getLineNumber() );

		tr.get( "IMPORT" );
		tr.get( "CSV" );

		Token t = tr.get( "SEPARATED", "PREPEND", "USING", "INTO" );

		if( t.equals( "SEPARATED" ) )
		{
			tr.get( "BY" );
			t = tr.get();
			if( t.equals( "TAB" ) )
				result.separator = '\t';
			else
			{
				if( t.length() != 1 )
					throw new CommandFileException( "Expecting [TAB] or one character, not [" + t + "]", tr.getLineNumber() );
				result.separator = t.getValue().charAt( 0 );
			}

			t = tr.get( "PREPEND", "USING", "INTO" );
		}

		if( t.equals( "PREPEND" ) )
		{
			tr.get( "LINENUMBER" );
			result.prependLineNumber = true;

			t = tr.get( "USING", "INTO" );
		}

		if( t.equals( "USING" ) )
		{
			t = tr.get( "PLBLOCK", "VALUESLIST" );
			if( t.equals( "PLBLOCK" ) )
				result.usePLBlock = true;
			else
				result.useValuesList = true;

			t = tr.get( "INTO" );
		}

		if( !t.equals( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tr.getLineNumber() );
		result.tableName = tr.get().toString();

		t = tr.get( "(", "VALUES", "DATA" );

		if( t.equals( "(" ) )
		{
			t = tr.get();
			if( t.equals( ")" ) || t.equals( "," ) )
				throw new CommandFileException( "Expecting a column name, not [" + t + "]", tr.getLineNumber() );
			columns.add( t.getValue() );
			t = tr.get( ",", ")" );
			while( !t.equals( ")" ) )
			{
				t = tr.get();
				if( t.equals( ")" ) || t.equals( "," ) )
					throw new CommandFileException( "Expecting a column name, not [" + t + "]", tr.getLineNumber() );
				columns.add( t.getValue() );
				t = tr.get( ",", ")" );
			}

			t = tr.get( "VALUES", "DATA" );
		}

		if( t.equals( "VALUES" ) )
		{
			tr.get( "(" );
			do
			{
				StringBuilder value = new StringBuilder();
				parseTill( tr, value, ",)", false );
				//System.out.println( "Value: " + value.toString() );
				values.add( value.toString() );

				t = tr.get( ",", ")" );
			}
			while( t.equals( "," ) );

			t = tr.get( "DATA" );
		}

		if( !t.equals( "DATA" ) )
			throw new CommandFileException( "Expecting [DATA], not [" + t + "]", tr.getLineNumber() );
		tr.getNewline();

		result.lineNumber = tr.getLineNumber();
		result.reader = tr.getReader();

		if( columns.size() > 0 )
			result.columns = columns.toArray( new String[ columns.size() ] );
		if( values.size() > 0 )
			result.values = values.toArray( new String[ values.size() ] );

		return result;
	}


	// Takes care of nested parenthesis
	static protected void parseTill( Tokenizer tr, StringBuilder result, String till, boolean includeInitialWhiteSpace )
	{
		Token t = tr.get();
		if( t == null )
			throw new CommandFileException( "Unexpected EOF", tr.getLineNumber() );
		if( t.length() == 1 )
			if( till.contains( t.getValue() ) )
				throw new CommandFileException( "Unexpected [" + t + "]", tr.getLineNumber() );

		if( includeInitialWhiteSpace )
			result.append( t.getWhiteSpace() );
		result.append( t.getValue() );

		while( true )
		{
			if( t.equals( "(" ) )
			{
				//System.out.println( "(" );
				parseTill( tr, result, ")", true );
				t = tr.get();
				Assert.isTrue( t.equals( ")" ) );
				//System.out.println( ")" );
				result.append( t.getWhiteSpace() );
				result.append( t.getValue() );
			}

			t = tr.get();
			if( t == null )
				throw new CommandFileException( "Unexpected EOF", tr.getLineNumber() );
			if( t.length() == 1 )
				if( till.contains( t.getValue() ) )
					break;

			result.append( t.getWhiteSpace() );
			result.append( t.getValue() );
		}

		tr.push( t );
	}


	static protected class Parsed
	{
		protected char separator = ',';
		protected boolean prependLineNumber;
		protected int lineNumber;
		protected boolean useValuesList;
		protected boolean usePLBlock;
		protected String tableName;
		protected String[] columns;
		protected String[] values;
		protected Reader reader;
	}


	static protected class Tokenizer
	{
		protected PushbackReader in;

		protected Tokenizer( Reader in, int lineNumber )
		{
			this.in = new PushbackReader( in, lineNumber );
		}

		protected boolean isWhitespace( int ch )
		{
			return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
		}

		protected boolean isControl( int ch )
		{
			return ch == '(' || ch == ')' || ch == ',' || ch == '"' || ch == '\'' || ch == ':';
		}

		protected Token get()
		{
			StringBuilder whiteSpace = new StringBuilder();
			int ch = this.in.read();
			while( ch != -1 && isWhitespace( ch ) )
			{
				whiteSpace.append( (char)ch );
				ch = this.in.read();
			}
			if( ch == '\'' || ch == '"' )
			{
				StringBuilder result = new StringBuilder( 16 );
				int stop = ch;
				while( true )
				{
					result.append( (char)ch );
					ch = this.in.read();
					if( ch == -1 )
						throw new CommandFileException( "Unexpected EOF", this.in.getLineNumber() );
					if( ch == '\n' )
						throw new CommandFileException( "Unexpected EOL", this.in.getLineNumber() );
					if( ch == stop )
					{
						result.append( (char)ch );
						ch = this.in.read();
						if( ch != stop )
						{
							this.in.push( ch );
							break;
						}
					}
				}
				return new Token( result.toString(), whiteSpace.toString() );
			}
			if( isControl( ch ) )
			{
				//System.out.println( "Token: " + (char)ch );
				return new Token( String.valueOf( (char)ch ), whiteSpace.toString() );
			}
			StringBuilder result = new StringBuilder( 16 );
			do
			{
				result.append( (char)ch );
				ch = this.in.read();
			}
			while( ch != -1 && !isWhitespace( ch ) && !isControl( ch ) );
			this.in.push( ch );
			if( result.length() == 0 )
				return null;
			//System.out.println( "Token: " + result.toString() );
			return new Token( result.toString(), whiteSpace.toString() );
		}

		protected Token get( String... objectives )
		{
			Assert.isTrue( objectives.length > 0 );
			Token token = get();
			boolean correct = false;
			for( String objective : objectives )
				if( token.equals( objective ) )
				{
					correct = true;
					break;
				}
			if( !correct )
			{
				if( objectives.length == 1 )
					throw new CommandFileException( "Expecting [" + objectives[ 0 ] + "], not [" + token + "]", token.isNewline() ? this.in.getLineNumber() - 1 : this.in.getLineNumber() );
				StringBuilder b = new StringBuilder( "Expecting one of" );
				for( String objective : objectives )
				{
					b.append( " [" );
					b.append( objective );
					b.append( ']' );
				}
				b.append( ", not [" );
				b.append( token );
				b.append( "]" );
				throw new CommandFileException( b.toString(), token.isNewline() ? this.in.getLineNumber() - 1 : this.in.getLineNumber() );
			}
			return token;
		}

		protected void getNewline()
		{
			int ch = this.in.read();
			while( ch != -1 && ch != '\n' && isWhitespace( ch ) )
				ch = this.in.read();
			if( ch != '\n' )
				throw new CommandFileException( "Expecting NEWLINE, not [" + (char)ch + "]", this.in.getLineNumber() );
		}

		protected void push( Token t )
		{
			//System.out.println( "Push : " + t );
			this.in.push( t.getValue() );
			this.in.push( t.getWhiteSpace() );
		}

		protected int getLineNumber()
		{
			return this.in.getLineNumber();
		}

		protected Reader getReader()
		{
			return this.in.getReader();
		}
	}


	// Case insensitive token
	static protected class Token
	{
		protected String value;
		protected String whiteSpace;

		protected Token( String value, String whiteSpace )
		{
			this.value = value;
			this.whiteSpace = whiteSpace;
		}

		protected String getValue()
		{
			return this.value;
		}

		protected String getWhiteSpace()
		{
			return this.whiteSpace;
		}

		protected boolean isNewline()
		{
			return this.value.charAt( 0 ) == '\n'; // Assume that if char 0 is a newline then the whole string is just the newline
		}

		protected boolean equals( String s )
		{
			return this.value.equalsIgnoreCase( s );
		}

		protected int length()
		{
			return this.value.length();
		}

		@Override
		public String toString()
		{
			return this.value;
		}
	}
}
