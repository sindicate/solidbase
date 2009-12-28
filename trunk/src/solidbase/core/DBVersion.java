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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.SAXException;


/**
 * This class represents the version information in the database. It is able to interpret the data and modify the data.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:17:41 PM
 */
public class DBVersion
{
	/**
	 * Need to refresh this instance from the data in the DBVERSION table.
	 */
	protected boolean stale = true;

	/**
	 * Does the DBVERSION table exist and does it contain a record?
	 */
	protected boolean versionRecordExists;

	/**
	 * Does the DBVERSIONLOG table exist?
	 */
	protected boolean logTableExists;

	/**
	 * Does the DBVERSION table contain a SPEC column?
	 */
	protected boolean specColumnExists;

	/**
	 * The current version of the database.
	 */
	protected String version;

	/**
	 * Is the database halfway a new version, and which version is that?
	 */
	protected String target;

	/**
	 * How many statements have been successfully executed for the current target? If the target is null, then not
	 * relevant, but will contain the number of statements executed for the current version.
	 */
	protected int statements;

	/**
	 * How do the DBVERSION tables look like.
	 */
	protected String spec;

	/**
	 * What's the database and schema that contains the DBVERSION tables.
	 */
	protected Database database;

	/**
	 * An instance of this class needs to now in which database the version tables can be found. The default
	 * connection of this database determines the schema where those tables reside.
	 * 
	 * @param database The database that contains the version tables, with its default connection determining the schema.
	 */
	protected DBVersion( Database database )
	{
		this.database = database;
	}

	/**
	 * Signal that this instance is most likely not up to date with the database. This happens when the database is
	 * initialized or upgraded to a new version of the DBVERSION tables.
	 */
	protected void setStale()
	{
		this.stale = true;
	}

	/**
	 * Gets the current version of the database. If the version table does not yet exist it returns null.
	 *
	 * @return the current version of the database.
	 */
	protected String getVersion()
	{
		if( this.stale )
			init();
		return this.version;
	}

	/**
	 * Gets the current target version of the database. This method will only return a non-null value when the database state is in between 2 versions.
	 *
	 * @return the current target version of the database.
	 */
	protected String getTarget()
	{
		if( this.stale )
			init();
		return this.target;
	}

	/**
	 * Gets the number of statements that have been executed to upgrade the database to the target version.
	 *
	 * @return the number of statements that have been executed.
	 */
	protected int getStatements()
	{
		if( this.stale )
			init();
		return this.statements;
	}

	/**
	 * Returns the specification version of the version tables.
	 * 
	 * @return the specification version of the version tables.
	 */
	protected String getSpec()
	{
		if( this.stale )
			init();
		return this.spec;
	}

	/**
	 * Initializes this instance by trying to read the version tables. It will succeed when one or all of the version tables does not exist.
	 */
	protected void init()
	{
		Assert.notNull( this.database.getDefaultUser(), "Default user is not set" );
		Assert.isTrue( this.stale );

		this.version = null;
		this.target = null;
		this.statements = 0;
		this.spec = null;

		Connection connection = this.database.getConnection();
		try
		{
			try
			{
				PreparedStatement statement = connection.prepareStatement( "SELECT * FROM DBVERSION" );
				try
				{
					ResultSet resultSet = statement.executeQuery(); // Resultset is closed when the statement is closed
					if( Util.hasColumn( resultSet, "SPEC" ) )
						this.specColumnExists = true;
					else
						Assert.isFalse( this.specColumnExists, "SPEC column in DBVERSION table has disappeared" );
					if( resultSet.next() )
					{
						this.version = resultSet.getString( "VERSION" );
						this.target = resultSet.getString( "TARGET" );
						this.statements = resultSet.getInt( "STATEMENTS" );
						if( this.specColumnExists )
							this.spec = resultSet.getString( "SPEC" );
						else
							this.spec = "1.0";
						Assert.isTrue( this.spec == "1.0" || this.spec.equals( "1.1" ) );
						Assert.isTrue( !resultSet.next() );

						Patcher.callBack.debug( "version=" + this.version + ", target=" + this.target + ", statements=" + this.statements );

						this.versionRecordExists = true;
					}
					else
					{
						Assert.isFalse( this.versionRecordExists, "Record in DBVERSION has disappeared" );
					}
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
				if( !( sqlState.equals( "42000" ) /* Oracle */|| sqlState.equals( "42S02" ) /* MySQL */
						|| sqlState.equals( "42X05" ) /* Derby */|| sqlState.equals( "S0002" ) /* HSQLDB */) )
					throw new SystemException( e );

				Assert.isFalse( this.versionRecordExists, "DBVERSION table has disappeared" );
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
				Assert.isFalse( this.logTableExists, "DBVERSIONLOG table has disappeared" );
			}
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

		this.stale = false;
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
		Assert.isTrue( statements > 0 );

		if( this.stale )
			init();

		if( this.versionRecordExists )
			execute( "UPDATE DBVERSION SET TARGET = ?, STATEMENTS = ?", new Object[] { target, statements } );
		else
		{
			execute( "INSERT INTO DBVERSION ( TARGET, STATEMENTS ) VALUES ( ?, ? )", new Object[] { target, statements } );
			this.versionRecordExists = true;
		}

		this.target = target;
		this.statements = statements;
	}

	/**
	 * Sets the current version.
	 *
	 * @param version The version.
	 */
	protected void setVersion( String version )
	{
		Assert.notEmpty( version, "Version must not be empty" );

		if( this.stale )
			init();

		if( this.versionRecordExists )
			execute( "UPDATE DBVERSION SET VERSION = ?, TARGET = NULL", new Object[] { version } );
		else
		{
			execute( "INSERT INTO DBVERSION ( VERSION, TARGET, STATEMENTS ) VALUES ( ?, NULL, 0 )", new Object[] { version } );
			this.versionRecordExists = true;
		}

		this.version = version;
		this.target = null;
	}

	/**
	 * Sets the current spec.
	 * 
	 * @param spec The spec.
	 */
	protected void setSpec( String spec )
	{
		Assert.notEmpty( spec, "Spec must not be empty" );

		if( this.stale )
			init();

		if( spec.equals( "1.0" ) )
		{
			Assert.isFalse( this.specColumnExists, "SPEC column should not exist in the DBVERSION table" );
		}
		else
		{
			Assert.isTrue( spec.equals( "1.1" ), "Only spec 1.0 or 1.1 allowed" );
			Assert.isTrue( this.specColumnExists, "SPEC column should exist in the DBVERSION table" );
			if( this.versionRecordExists )
				execute( "UPDATE DBVERSION SET SPEC = ?", new Object[] { spec } );
			else
			{
				execute( "INSERT INTO DBVERSION ( STATEMENTS, SPEC ) VALUES ( 0, ? )", new Object[] { spec } );
				this.versionRecordExists = true;
			}
		}

		this.spec = spec;
	}

	/**
	 * Adds a log record to the version log table.
	 * 
	 * @param type The type of the log entry.
	 * @param source The source version.
	 * @param target The target version.
	 * @param count The statement count.
	 * @param command The executed statement.
	 * @param result The result of executing the statement.
	 */
	protected void log( String type, String source, String target, int count, String command, String result )
	{
		Assert.notEmpty( target, "target must not be empty" );

		if( this.stale )
			init();

		if( !this.logTableExists )
			return;

		// Trim strings, maximum length for VARCHAR2 in Oracle is 4000 !BYTES!
		// Trim more, to make room for UTF8 bytes
		if( command != null && command.length() > 3000 )
			command = command.substring( 0, 3000 );
		if( result != null && result.length() > 3000 )
			result = result.substring( 0, 3000 );

		if( "1.1".equals( this.spec ) )
			execute( "INSERT INTO DBVERSIONLOG ( TYPE, SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT ) VALUES ( ?, ?, ?, ?, ?, ?, ? )",
					new Object[] { type, source, target, count, new Timestamp( System.currentTimeMillis() ), command, result } );
		else
			execute( "INSERT INTO DBVERSIONLOG ( SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT ) VALUES ( ?, ?, ?, ?, ?, ? )",
					new Object[] { source, target, count, new Timestamp( System.currentTimeMillis() ), command, result } );
	}

	/**
	 * Adds a log record to the version log table.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @param count The statement count.
	 * @param command The executed statement.
	 * @param e The exception.
	 */
	protected void log( String source, String target, int count, String command, Exception e )
	{
		Assert.notNull( e, "exception must not be null" );

		StringWriter buffer = new StringWriter();
		e.printStackTrace( new PrintWriter( buffer ) );

		log( "S", source, target, count, command, buffer.toString() );
	}

	/**
	 * Adds a log record to the version log table.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @param count The statement count.
	 * @param command The executed statement.
	 * @param e The sql exception.
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

		log( "S", source, target, count, command, buffer.toString() );
	}

	/**
	 * Log a complete block.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @param count The statement count.
	 */
	protected void logComplete( String source, String target, int count )
	{
		log( "B", source, target, count, null, "1.1".equals( this.spec ) ? "COMPLETE" : "COMPLETED VERSION " + target );
	}

	/**
	 * Dumps the current log in XML format to the given output stream, with the given character set.
	 *
	 * @param out The outputstream.
	 * @param charSet
	 */
	protected void logToXML( OutputStream out, Charset charSet )
	{
		// This method does not care about staleness

		try
		{
			Connection connection = this.database.getConnection();
			Statement stat = connection.createStatement();
			try
			{
				ResultSet result = stat.executeQuery( "SELECT TYPE, SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT FROM DBVERSIONLOG ORDER BY STAMP" );

				XMLOutputFactory xof = XMLOutputFactory.newInstance();
				XMLStreamWriter xtw = xof.createXMLStreamWriter( new OutputStreamWriter( out, charSet ) );
				xtw.writeStartDocument("UTF-8", "1.0");
				xtw.writeStartElement( "log" );
				while( result.next() )
				{
					xtw.writeStartElement( "command" );
					xtw.writeAttribute( "type", result.getString( 1 ) );
					xtw.writeAttribute( "source", result.getString( 2 ) );
					xtw.writeAttribute( "target", result.getString( 3 ) );
					xtw.writeAttribute( "count", String.valueOf( result.getInt( 4 ) ) );
					xtw.writeAttribute( "stamp", String.valueOf( result.getTimestamp( 5 ) ) );
					String sql = result.getString( 6 );
					if( sql != null )
						xtw.writeCharacters( sql );
					String res = result.getString( 7 );
					if( res != null )
					{
						xtw.writeStartElement( "result" );
						xtw.writeCharacters( res );
						xtw.writeEndElement();
					}
					xtw.writeEndElement();
				}
				xtw.writeEndElement();
				xtw.writeEndDocument();
				xtw.close();
			}
			finally
			{
				stat.close();
				connection.commit();
			}
		}
		catch( XMLStreamException e )
		{
			throw new SystemException( e );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Checks if a specific version is in the history of this database.
	 * 
	 * @param version The version to be checked.
	 * @return True if the version is part of this database's history, false otherwise.
	 */
	protected boolean logContains( String version )
	{
		Assert.isFalse( this.stale );

		String sql;
		if( "1.1".equals( this.spec ) )
			sql = "SELECT 1 FROM DBVERSIONLOG WHERE TYPE = 'B' AND TARGET = '" + version + "' AND RESULT = 'COMPLETE'";
		else
			sql = "SELECT 1 FROM DBVERSIONLOG WHERE RESULT = 'COMPLETED VERSION " + version + "'";

		Connection connection = this.database.getConnection();
		try
		{
			PreparedStatement stat = connection.prepareStatement( sql );
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

	/**
	 * Execute the given sql with the given parameters. It asserts that exactly one record is updated.
	 * 
	 * @param sql The sql to be executed.
	 * @param parameters The parameters for the sql.
	 */
	protected void execute( String sql, Object[] parameters )
	{
		try
		{
			Connection connection = this.database.getConnection();
			PreparedStatement statement = connection.prepareStatement( sql );
			ParameterMetaData meta = statement.getParameterMetaData();
			int i = 1;
			for( Object parameter : parameters )
				if( parameter == null )
					statement.setNull( i, meta.getParameterType( i++ ) ); // Derby 10.2 wants this
				else
					statement.setObject( i++, parameter );
			try
			{
				int modified = statement.executeUpdate();
				Assert.isTrue( modified == 1, "Expecting 1 record to be updated, not " + modified );
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

	// TODO Make this faster with an IN.
	protected void downgradeHistory( Collection< String > versions )
	{
		Assert.isTrue( versions.size() > 0 );
		try
		{
			Connection connection = this.database.getConnection();
			PreparedStatement statement = connection.prepareStatement( "UPDATE DBVERSIONLOG SET RESULT = 'DOWNGRADED' WHERE TYPE = 'B' AND TARGET = ? AND RESULT = 'COMPLETE'" );
			boolean commit = false;
			try
			{
				for( String version : versions )
				{
					statement.setString( 1, version );
					int modified = statement.executeUpdate();
					Assert.isTrue( modified <= 1, "Expecting not more than 1 record to be updated, not " + modified );
				}
				commit = true;
			}
			finally
			{
				statement.close();
				if( commit )
					connection.commit();
				else
					connection.rollback();
			}
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}
}
