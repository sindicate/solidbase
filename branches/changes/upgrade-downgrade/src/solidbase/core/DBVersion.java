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

/*
 * TABLES:
 *
 * DBVERSION ( VERSION, TARGET, STATEMENTS ), 1 record
 *
 *     Examples:
 *     DHL TTS 2.0.11, <NULL>, 10 : version is complete, it took 10 statements to get there
 *     DHL TTS 2.0.11, DHL TTS 2.0.12, 4 : version is not complete, 4 statements already executed
 *
 * DBVERSIONLOG ( VERSION, TARGET, STATEMENT, STAMP, SQL, RESULT )
 *
 *     Version jumps:
 *     DHL TTS 2.0.11, <NULL>, <NULL>, 2006-03-27 13:56:00, <NULL>, <NULL>
 *     DHL TTS 2.0.12, <NULL>, <NULL>, 2006-03-27 13:56:00, <NULL>, <NULL>
 *
 *     Individual statements:
 *     DHL TTS 2.0.11, DHL TTS 2.0.12, 5, 2006-03-27 13:56:00, CREATE TABLE ..., TABLE ALREADY EXISTS
 */

package solidbase.core;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;


/**
 * This class represents the version information in the database. It is able to interpret the data and modify the data.
 *
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:17:41 PM
 */
public class DBVersion
{
	protected boolean read; // read tried
	protected boolean valid; // read succeeded
	protected boolean versionTableExists;
	protected boolean logTableExists;
	protected boolean specColumnExists;

	protected String version;
	protected String target;
	protected int statements;
	protected String spec;

	protected Database database;


	protected DBVersion( Database database )
	{
		this.database = database;
	}

	/**
	 * Gets the current version of the database. Will return null when the initial upgrade block is not finished yet.
	 *
	 * @return the current version of the database.
	 */
	protected String getVersion()
	{
		if( !this.read )
			read();

		Assert.isTrue( this.valid );

		if( !this.versionTableExists )
			return null;

		// this.version might still be null when creation of the DBVERSIONLOG table fails
		return this.version;
	}

	/**
	 * Gets the current target version of the database. This method will only return a non-null value when the database state is in between 2 versions.
	 *
	 * @return the current target version of the database.
	 */
	protected String getTarget()
	{
		if( !this.read )
			read();

		Assert.isTrue( this.valid );

		if( !this.versionTableExists )
			return null;

		return this.target;
	}

	/**
	 * Gets the number of statements that have been executed to upgrade the database to the target version.
	 *
	 * @return the number of statements that have been executed.
	 */
	protected int getStatements()
	{
		if( !this.read )
			read();

		Assert.isTrue( this.valid );

		if( !this.versionTableExists )
			return 0;

		return this.statements;
	}

	/**
	 * Refreshes the data from the database. Is automatically called if needed by {@link #getVersion()}, {@link #getTarget()} and {@link #getStatements()}.
	 *
	 */
	protected void read()
	{
		Assert.notNull( this.database.getDefaultUser(), "Default user is not set" );
		this.read = true;

		//if( this.valid )
		//Assert.isTrue( this.versionTableExists );

		Connection connection = this.database.getConnection( this.database.getDefaultUser() );
		try
		{
			try
			{
				PreparedStatement statement = connection.prepareStatement( "SELECT VERSION, TARGET, STATEMENTS FROM DBVERSION" );
				try
				{
					ResultSet resultSet = statement.executeQuery(); // Resultset is closed when the statement is closed
					Assert.isTrue( resultSet.next() );
					this.version = resultSet.getString( 1 );
					this.target = resultSet.getString( 2 );
					this.statements = resultSet.getInt( 3 );
					Assert.isTrue( !resultSet.next() );

					Patcher.callBack.debug( "version=" + this.version + ", target=" + this.target + ", statements=" + this.statements );

					this.versionTableExists = true;
					this.valid = true;
				}
				finally
				{
					statement.close();
				}
			}
			catch( SQLException e )
			{
				String sqlState = e.getSQLState();
				// TODO Make this configurable
				if( sqlState.equals( "42000" ) /* Oracle */ || sqlState.equals( "42S02" ) /* MySQL */  || sqlState.equals( "42X05" ) /* Derby */  || sqlState.equals( "S0002" ) /* HSQLDB */ ) // Table not found error
				{
					Assert.isFalse( this.versionTableExists );
				}
				else
					throw new SystemException( e );
			}

			if( this.versionTableExists )
			{
				try
				{
					PreparedStatement statement = connection.prepareStatement( "SELECT SPEC FROM DBVERSION" );
					try
					{
						ResultSet resultSet = statement.executeQuery(); // Resultset is closed when the statement is closed
						Assert.isTrue( resultSet.next() );
						this.specColumnExists = true;
						this.spec = resultSet.getString( 1 );
						Assert.isTrue( !resultSet.next() );
					}
					finally
					{
						statement.close();
					}
				}
				catch( SQLException e )
				{
					String sqlState = e.getSQLState();
					// TODO Make this configurable
					// TODO Add Oracle, MySql & Derby
					if( sqlState.equals( "S0022" ) /* HSQLDB */ ) // Column not found error
					{
						Assert.isFalse( this.specColumnExists );
						this.spec = "1.0";
					}
					else
						throw new SystemException( e );
				}

				Patcher.callBack.debug( "spec=" + this.spec );
				if( !( this.spec.equals( "1.0" ) || this.spec.equals( "1.1" ) ) )
					Assert.fail( "Unrecognized spec " + this.spec );
			}

			try
			{
				PreparedStatement statement = connection.prepareStatement( "SELECT * FROM DBVERSIONLOG" );
				try
				{
					statement.executeQuery();
					this.logTableExists = true;
				}
				finally
				{
					statement.close();
				}
			}
			catch( SQLException e )
			{
				String sqlState = e.getSQLState();
				if( !( sqlState.equals( "42000" ) /* Oracle */ || sqlState.equals( "42S02" ) /* MySQL */ || sqlState.equals( "42X05" ) /* Derby */ || sqlState.equals( "S0002" ) /* HSQLDB */ ) )
					throw new SystemException( e );
				// version log table does not exist
			}

			this.valid = true;
		}
		finally
		{
			try
			{
				connection.commit();
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
	}

	/**
	 * Sets the number of statements executed and the target version.
	 *
	 * @param target The target version.
	 * @param statements The number of statements executed.
	 */
	protected void setProgress( String target, int statements )
	{
		Assert.notEmpty( target, "Target must not be empty" );
		Assert.isTrue( this.valid );
		Assert.isTrue( this.versionTableExists );

		try
		{
			Connection connection = this.database.getConnection( this.database.getDefaultUser() );
			PreparedStatement statement;
			if( this.versionTableExists )
				statement = connection.prepareStatement( "UPDATE DBVERSION SET TARGET = ?, STATEMENTS = ?" );
			else
			{
				// Presume that the table has been created by the first SQL statement in the patch
				statement = connection.prepareStatement( "INSERT INTO DBVERSION ( TARGET, STATEMENTS, SPEC ) VALUES ( ?, ?, '1.1' )" );
			}
			try
			{
				statement.setString( 1, target );
				statement.setInt( 2, statements );
				int modified = statement.executeUpdate();
				Assert.isTrue( modified == 1, "Expecting 1 record to be updated, not " + modified );
			}
			finally
			{
				statement.close();
				connection.commit(); // You can commit even if it fails. Only 1 update done.
			}

			this.versionTableExists = true;

			this.target = target;
			this.statements = statements;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Sets the current version.
	 *
	 * @param version The version.
	 */
	protected void setVersion( String version )
	{
		Assert.notEmpty( version, "Version must not be empty" );
		Assert.isTrue( this.versionTableExists, "Version table does not exist" );

		try
		{
			Connection connection = this.database.getConnection( this.database.getDefaultUser() );
			PreparedStatement statement = connection.prepareStatement( "UPDATE DBVERSION SET VERSION = ?, TARGET = NULL" );
			try
			{
				statement.setString( 1, version );
				int modified = statement.executeUpdate();
				Assert.isTrue( modified == 1, "Expecting 1 record to be updated, not " + modified );
			}
			finally
			{
				statement.close();
				connection.commit(); // You can commit even if it fails. Only 1 update done.
			}

			this.version = version;
			this.target = null;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Adds a log record to the version log table.
	 *
	 * @param source
	 * @param target
	 * @param count
	 * @param command
	 * @param result
	 */
	protected void log( String source, String target, int count, String command, String result )
	{
		if( !this.logTableExists )
			return;

		//		Assert.notEmpty( source, "source must not be empty" );
		Assert.notEmpty( target, "target must not be empty" );
		//		Assert.notEmpty( command, "command must not be empty" );

		// Trim strings, maximum length for VARCHAR2 in Oracle is 4000 !BYTES!
		// Trim more, to make room for UTF8 bytes
		if( command != null && command.length() > 3000 )
			command = command.substring( 0, 3000 );
		if( result != null && result.length() > 3000 )
			result = result.substring( 0, 3000 );

		try
		{
			Connection connection = this.database.getConnection( this.database.getDefaultUser() );
			PreparedStatement statement = connection.prepareStatement( "INSERT INTO DBVERSIONLOG ( SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT ) VALUES ( ?, ?, ?, ?, ?, ? )" );
			try
			{
				statement.setString( 1, StringUtils.stripToNull( source ) );
				statement.setString( 2, target );
				statement.setInt( 3, count );
				statement.setTimestamp( 4, new Timestamp( System.currentTimeMillis() ) );
				statement.setString( 5, StringUtils.stripToNull( command ) );
				statement.setString( 6, StringUtils.stripToNull( result ) );
				statement.executeUpdate();
			}
			finally
			{
				statement.close();
				connection.commit(); // You can commit even if it fails. Only 1 update done.
			}
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Adds a log record to the version log table.
	 *
	 * @param source
	 * @param target
	 * @param count
	 * @param command
	 * @param e
	 */
	protected void log( String source, String target, int count, String command, Exception e )
	{
		Assert.notNull( e, "exception must not be null" );

		StringWriter buffer = new StringWriter();
		e.printStackTrace( new PrintWriter( buffer ) );

		log( source, target, count, command, buffer.toString() );
	}

	/**
	 * Adds a log record to the version log table.
	 *
	 * @param source
	 * @param target
	 * @param count
	 * @param command
	 * @param e
	 */
	protected void logSQLException( String source, String target, int count, String command, SQLException e )
	{
		Assert.notNull( e, "exception must not be null" );

		StringBuffer buffer = new StringBuffer();

		while( true )
		{
			buffer.append( e.getSQLState() );
			buffer.append( ": " );
			buffer.append( e.getMessage() );
			e = e.getNextException();
			if( e == null )
				break;
			buffer.append( "\n" );
		}

		log( source, target, count, command, buffer.toString() );
	}

	/**
	 * Dumps the current log in XML format to the given output stream.
	 *
	 * @param out The outputstream.
	 */
	protected void logToXML( OutputStream out, Charset charSet )
	{
		try
		{
			Connection connection = this.database.getConnection( this.database.getDefaultUser() );
			Statement stat = connection.createStatement();
			try
			{
				ResultSet result = stat.executeQuery( "SELECT SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT FROM DBVERSIONLOG ORDER BY ID" );

				OutputFormat format = new OutputFormat( "XML", charSet.name(), true );
				XMLSerializer serializer = new XMLSerializer( out, format );

				serializer.startDocument();
				serializer.startElement( null, null, "log", null );

				while( result.next() )
				{
					AttributesImpl attributes = new AttributesImpl();
					attributes.addAttribute( null, null, "source", null, result.getString( 1 ) );
					attributes.addAttribute( null, null, "target", null, result.getString( 2 ) );
					attributes.addAttribute( null, null, "count", null, String.valueOf( result.getInt( 3 ) ) );
					attributes.addAttribute( null, null, "stamp", null, String.valueOf( result.getTimestamp( 4 ) ) );
					serializer.startElement( null, null, "command", attributes );
					String sql = result.getString( 5 );
					if( sql != null )
						serializer.characters( sql.toCharArray(), 0, sql.length() );
					String res = result.getString( 6 );
					if( res != null )
					{
						serializer.startElement( null, null, "result", null );
						serializer.characters( res.toCharArray(), 0, res.length() );
						serializer.endElement( null, null, "result" );
					}
					serializer.endElement( null, null, "command" );
				}

				serializer.endElement( null, null, "log" );
				serializer.endDocument();
			}
			finally
			{
				stat.close();
				connection.commit();
			}
		}
		catch( SAXException e )
		{
			throw new SystemException( e );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	protected boolean logContains( String version )
	{
		Connection connection = this.database.getConnection( this.database.getDefaultUser() );
		try
		{
			PreparedStatement stat = connection.prepareStatement( "SELECT ID FROM DBVERSIONLOG WHERE RESULT = 'COMPLETED VERSION " + version + "'" );
			try
			{
				ResultSet result = stat.executeQuery();
				return result.next();
			}
			finally
			{
				stat.close();
				connection.commit();
			}
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}
}