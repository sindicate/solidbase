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

package ronnie.dbpatcher.core;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;
import com.logicacmg.idt.commons.util.StringUtil;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * This class represents the version information in the database. It is able to interpret the data and modify the data.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:17:41 PM
 */
public class DBVersion
{
	protected boolean read; // read tried
	protected boolean valid; // read succeeded
	protected boolean versionTableExists;
	protected boolean logTableExists;
	
	protected String version;
	protected String target;
	protected int statements;
	protected String user;
	
	protected Database database;
	
	protected DBVersion( Database database )
	{
		this.database = database;
	}
	
	/**
	 * Gets the current version of the database. If the version table does not yet exist it return null.
	 * 
	 * @return the current version of the database. Will be null if and only if the version table does not exist.
	 */
	protected String getVersion()
	{
		if( !this.read )
			read();
		
		Assert.isTrue( this.valid );
		
		if( !this.versionTableExists )
			return null;
		
		Assert.notNull( this.version );
		
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
		Assert.notNull( this.user, "User is not set" );
		this.read = true;
		
		this.versionTableExists = false;
		this.logTableExists = false;
		
		try
		{
			PreparedStatement statement = this.database.getConnection( this.user ).prepareStatement( "SELECT VERSION, TARGET, STATEMENTS FROM DBVERSION" );
			ResultSet resultSet = statement.executeQuery();
			try
			{
				this.versionTableExists = true;
				
				Assert.isTrue( resultSet.next() );
				this.version = resultSet.getString( 1 );
				this.target = resultSet.getString( 2 );
				this.statements = resultSet.getInt( 3 );
				Assert.isTrue( !resultSet.next() );
				
				Patcher.callBack.debug( "version=" + this.version + ", target=" + this.target + ", statements=" + this.statements );
				
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
			if( sqlState.equals( "42000" ) /* Oracle */ || sqlState.equals( "42X05" ) /* Derby */ )
				this.valid = true;
			else
				throw new SystemException( e );
		}
		
		try
		{
			PreparedStatement statement = this.database.getConnection( this.user ).prepareStatement( "SELECT * FROM DBVERSIONLOG" );
			statement.executeQuery();
			try
			{
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
			if( !( sqlState.equals( "42000" ) /* Oracle */ || sqlState.equals( "42X05" ) /* Derby */ ) )
				throw new SystemException( e );
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
		
		try
		{
			PreparedStatement statement;
			if( this.versionTableExists )
				statement = this.database.getConnection( this.user ).prepareStatement( "UPDATE DBVERSION SET TARGET = ?, STATEMENTS = ?" );
			else
			{
				// Presume that the table has been created by the first SQL statement in the patch
				statement = this.database.getConnection( this.user ).prepareStatement( "INSERT INTO DBVERSION ( TARGET, STATEMENTS ) VALUES ( ?, ? )" );
			}
			statement.setString( 1, target );
			statement.setInt( 2, statements );
			int modified = statement.executeUpdate(); // autocommit is on
			Assert.isTrue( modified == 1, "Expecting 1 record to be updated, not " + modified );
			statement.close();
			
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
			PreparedStatement statement = this.database.getConnection( this.user ).prepareStatement( "UPDATE DBVERSION SET VERSION = ?, TARGET = NULL" );
			statement.setString( 1, version );
			int modified = statement.executeUpdate(); // autocommit is on
			Assert.isTrue( modified == 1, "Expecting 1 record to be updated, not " + modified );
			statement.close();
			
			this.version = version;
			this.target = null;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * The user/owner/schema of the version tables.
	 * 
	 * @param user The user.
	 */
	protected void setUser( String user )
	{
		this.user = user;
	}

	/**
	 * Gets the user/owner/schema of the version tables.
	 * 
	 * @return
	 */
	protected String getUser()
	{
		return this.user;
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
		
		// Trim strings, maximum length for VARCHAR2 is 4000
		if( command != null && command.length() > 4000 )
			command = command.substring( 0, 4000 );
		if( result != null && result.length() > 4000 )
			result = result.substring( 0, 4000 );
		
		try
		{
			PreparedStatement statement = this.database.getConnection( getUser() ).prepareStatement( "INSERT INTO DBVERSIONLOG ( SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT ) VALUES ( ?, ?, ?, ?, ?, ? )" );
			statement.setString( 1, StringUtil.emptyToNull( source ) );
			statement.setString( 2, target );
			statement.setInt( 3, count );
			statement.setTimestamp( 4, new Timestamp( System.currentTimeMillis() ) );
			statement.setString( 5, StringUtil.emptyToNull( command ) );
			statement.setString( 6, StringUtil.emptyToNull( result ) );
			statement.executeUpdate(); // autocommit is on
			statement.close();
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
	protected void logToXML( OutputStream out )
	{
		try
		{
			ResultSet result = this.database.getConnection( getUser() ).createStatement().executeQuery( "SELECT SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT FROM DBVERSIONLOG ORDER BY ID" );
			
			OutputFormat format = new OutputFormat( "XML", "ISO-8859-1", true );
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
		catch( SAXException e )
		{
			throw new SystemException( e );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}
}
