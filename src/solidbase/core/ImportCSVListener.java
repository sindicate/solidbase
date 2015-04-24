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

package solidbase.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ImportCSVListener extends CommandListener
{
	static private final Pattern importPattern = Pattern.compile( "\\s*IMPORT\\s+CSV\\s+(SEPERATED BY (\\S|TAB)\\s+)?INTO\\s+([^\\s]+)(\\s+AS\\s+PLBLOCK)?(\\s+AS\\s+VALUESLIST)?\\n(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	/**
	 * Constructor.
	 */
	public ImportCSVListener()
	{
		super();
	}

	@Override
	protected boolean execute( Database database, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = importPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		String sep = matcher.group( 2 );
		char seperator;
		if( sep == null )
			seperator = ',';
		else if( sep.equalsIgnoreCase( "TAB" ) )
			seperator = '\t';
		else
		{
			Assert.isTrue( sep.length() == 1, "Seperator should be 1 character long", command.getLineNumber() );
			seperator = sep.charAt( 0 );
		}
		String tableName = matcher.group( 3 );
		String asBlock = matcher.group( 4 );
		String asValues = matcher.group( 5 );
		String data = matcher.group( 6 );

		Connection connection = database.getConnection();
		Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
		PreparedStatement statement = null;

		boolean commit = false;
		try
		{
			CSVReader reader = new CSVReader( new StringReader( data ), seperator, '"', "#", true, false, true );
			try
			{
				String[] line;
				try
				{
					line = reader.getAllFieldsInLine();
				}
				catch( EOFException e )
				{
					line = null;
				}
				if( asBlock != null )
				{
					StringBuilder sql = new StringBuilder();
					sql.append( "BEGIN\n" );
					while( line != null )
					{
						sql.append( "INSERT INTO " );
						sql.append( tableName );
						sql.append( " VALUES (" );
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
					}
					sql.append( "END;\n" );
					String s = sql.toString();
					statement = connection.prepareStatement( sql.toString() );
					command.setCommand( s );
					statement.executeUpdate();
				}
				else if( asValues != null )
				{
					StringBuilder sql = new StringBuilder();
					sql.append( "INSERT INTO " );
					sql.append( tableName );
					sql.append( " VALUES\n" );
					boolean comma = false;
					while( line != null )
					{
						if( comma )
							sql.append( ',' );
						sql.append( '(' );
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
					}
					sql.append( '\n' );
					String s = sql.toString();
					statement = connection.prepareStatement( sql.toString() );
					command.setCommand( s );
					statement.executeUpdate();
				}
				else
				{
					while( line != null )
					{
						if( statement == null )
						{
							StringBuilder sql = new StringBuilder().append( "INSERT INTO " );
							sql.append( tableName );
							sql.append( " VALUES (?" );
							for( int i = line.length; i > 1; i-- )
								sql.append( ",?" );
							sql.append( ')' );
							statement = connection.prepareStatement( sql.toString() );
						}
						int i = 0;
						for( String field : line )
							statement.setString( ++i, field );
						statement.executeUpdate();
						try
						{
							line = reader.getAllFieldsInLine();
						}
						catch( EOFException e )
						{
							line = null;
						}
					}
				}
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			commit = true;
		}
		finally
		{
			if( statement != null )
				statement.close();
			if( commit )
				connection.commit();
			else
				connection.rollback();
		}

		return true;
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
}
