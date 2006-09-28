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

import com.lcmg.rbloois.SystemException;
import com.lcmg.rbloois.util.Assert;
import com.lcmg.rbloois.util.StringUtil;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:17:41 PM
 */
public class DBVersionLog
{
	static protected void log( String source, String target, int count, String command, String result )
	{
		if( !DBVersion.tableexists )
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
			PreparedStatement statement = Database.getConnection( DBVersion.getUser() ).prepareStatement( "INSERT INTO DBVERSIONLOG ( SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT ) VALUES ( ?, ?, ?, ?, ?, ? )" );
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

	static protected void log( String source, String target, int count, String command, Exception e )
	{
		Assert.notNull( e, "exception must not be null" );
		
		StringWriter buffer = new StringWriter();
		e.printStackTrace( new PrintWriter( buffer ) );
		
		log( source, target, count, command, buffer.toString() );
	}

	static protected void logSQLException( String source, String target, int count, String command, SQLException e )
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
	
	static protected void logToXML( OutputStream out )
	{
		try
		{
			ResultSet result = Database.getConnection( DBVersion.getUser() ).createStatement().executeQuery( "SELECT SOURCE, TARGET, STATEMENT, STAMP, COMMAND, RESULT FROM DBVERSIONLOG ORDER BY ID" );
			
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
