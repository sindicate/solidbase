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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	static private final String syntax = "IMPORT CSV [SEPARATED BY TAB|<char>] [PREPEND LINENUMBER] [USING PLBLOCK|VALUESLIST] INTO <table> [(<colums>)] DATA <newline> <data>";


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
				String[] columns = parsed.columns.toArray( new String[ parsed.columns.size() ] );
				if( columns.length == 0 )
					columns = null;
				if( parsed.usePLBlock )
					importUsingPLBlock( command, connection, reader, parsed.tableName, columns, parsed.prependLineNumber, parsed.lineNumber, line );
				else if( parsed.useValuesList )
					importUsingValuesList( command, connection, reader, parsed.tableName, columns, parsed.prependLineNumber, parsed.lineNumber, line );
				else
					importNormal( command, connection, reader, parsed.tableName, columns, parsed.prependLineNumber, parsed.lineNumber, line );

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
	 * @param tableName The table name to import into.
	 * @param columns The optional column list.
	 * @param prependLineNumber Use the line number as the first value.
	 * @param lineNumber The line number of the import command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	protected void importUsingPLBlock( Command command, Connection connection, CSVReader reader, String tableName, String[] columns, boolean prependLineNumber, int lineNumber, String[] line ) throws SQLException, IOException
	{
		StringBuilder sql = new StringBuilder();
		sql.append( "BEGIN\n" );
		while( line != null )
		{
			sql.append( "INSERT INTO " );
			sql.append( tableName );
			if( columns != null )
			{
				sql.append( " (" );
				int i = 0;
				sql.append( columns[ i++ ] );
				for( int j = prependLineNumber ? 0 : 1; j < line.length; j++ )
				{
					sql.append( ',' );
					sql.append( columns[ i++ ] );
				}
				sql.append( ')' );
			}
			sql.append( " VALUES (" );
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
			sql.append( ");\n" );
			try
			{
				line = reader.getAllFieldsInLine();
			}
			catch( EOFException e )
			{
				line = null;
			}
			lineNumber++;
		}
		sql.append( "END;\n" );
		String s = sql.toString();
		command.setCommand( s );
		PreparedStatement statement = connection.prepareStatement( s );
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
	 * @param tableName The table name to import into.
	 * @param columns The optional column list.
	 * @param prependLineNumber Use the line number as the first value.
	 * @param lineNumber The line number of the import command.
	 * @param line The first line of data read.
	 * @throws SQLException Whenever SQL execution throws it.
	 * @throws IOException Whenever CSV reading throws it.
	 */
	protected void importNormal( @SuppressWarnings( "unused" ) Command command, Connection connection, CSVReader reader, String tableName, String[] columns, boolean prependLineNumber, int lineNumber, String[] line ) throws SQLException, IOException
	{
		Map< Integer, PreparedStatement > statementCache = new HashMap();
		try
		{
			while( true )
			{
				PreparedStatement statement = statementCache.get( line.length );
				if( statement == null )
				{
					StringBuilder sql = new StringBuilder().append( "INSERT INTO " );
					sql.append( tableName );
					if( columns != null )
					{
						sql.append( " (" );
						int i = 0;
						sql.append( columns[ i++ ] );
						for( int j = prependLineNumber ? 0 : 1; j < line.length; j++ )
						{
							sql.append( ',' );
							sql.append( columns[ i++ ] );
						}
						sql.append( ')' );
					}
					sql.append( " VALUES (?" );
					if( prependLineNumber )
						sql.append( ",?" );
					for( int i = line.length; i > 1; i-- )
						sql.append( ",?" );
					sql.append( ')' );

					statement = connection.prepareStatement( sql.toString() );
					statementCache.put( line.length, statement );
				}
				else
					statement.clearParameters();

				int i = 0;
				if( prependLineNumber )
					statement.setInt( ++i, lineNumber );
				for( String field : line )
					statement.setString( ++i, field );
				System.out.println( statement.toString() );
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
			for( PreparedStatement statement : statementCache.values() )
				statement.close();
		}
	}


	/**
	 * Converts ' to ''.
	 * 
	 * @param s The sql to escape.
	 * @return Escaped sql.
	 */
	protected String escape( String s )
	{
		return s.replaceAll( "'", "''" );
	}


	protected Parsed parse( Command command )
	{
		Parsed result = new Parsed();

		Tokenizer tr = new Tokenizer( new StringReader( command.getCommand() ), command.getLineNumber() );
		tr.get( "IMPORT" );
		tr.get( "CSV" );
		Token t = tr.get();
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
			t = tr.get();
		}
		if( t.equals( "PREPEND" ) )
		{
			tr.get( "LINENUMBER" );
			result.prependLineNumber = true;
			t = tr.get();
		}
		if( t.equals( "USING" ) )
		{
			t = tr.get( "PLBLOCK", "VALUESLIST" );
			if( t.equals( "PLBLOCK" ) )
				result.usePLBlock = true;
			else
				result.useValuesList = true;
			t = tr.get();
		}
		if( !t.equals( "INTO" ) )
			throw new CommandFileException( "Expecting [INTO], not [" + t + "]", tr.getLineNumber() );
		result.tableName = tr.get().toString();
		t = tr.get();
		if( t.equals( "(" ) )
		{
			t = tr.get();
			if( t.equals( ")" ) )
				throw new CommandFileException( "Expecting a column name, not [" + t + "]", tr.getLineNumber() );
			result.columns.add( t.getValue() );
			t = tr.get( ",", ")" );
			while( !t.equals( ")" ) )
			{
				t = tr.get();
				if( t.equals( "," ) )
					throw new CommandFileException( "Expecting a column name, not [" + t + "]", tr.getLineNumber() );
				result.columns.add( t.getValue() );
				t = tr.get( ",", ")" );
			}
			t = tr.get();
		}
		if( !t.equals( "DATA" ) )
			throw new CommandFileException( "Expecting [DATA], not [" + t + "]", tr.getLineNumber() );
		tr.getNewline();
		result.lineNumber = tr.getLineNumber();
		result.reader = tr.getReader();

		return result;
	}

	static protected class Parsed
	{
		protected char separator = ',';
		protected boolean prependLineNumber;
		protected int lineNumber;
		protected boolean useValuesList;
		protected boolean usePLBlock;
		protected String tableName;
		protected List< String > columns = new ArrayList< String >();
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

		protected Token get()
		{
			int ch = this.in.read();
			while( ch != -1 && isWhitespace( ch ) )
				ch = this.in.read();
			if( ch == '(' || ch == ')' || ch == ',' || ch == '"' || ch == '\'' )
				return new Token( (char)ch );
			StringBuilder result = new StringBuilder( 16 );
			while( ch != -1 && !isWhitespace( ch ) )
			{
				result.append( (char)ch );
				ch = this.in.read();
			}
			this.in.push( ch );
			if( result.length() == 0 )
				return null;
			return new Token( result.toString() );
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

		protected Token( String value )
		{
			this.value = value;
		}

		protected Token( char value )
		{
			this.value = new String( new char[] { value } );
		}

		protected String getValue()
		{
			return this.value;
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
