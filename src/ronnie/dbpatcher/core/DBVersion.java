/*
 * TABLES:
 * 
 * DBVERSION ( VERSION, TARGET, STATEMENTS ), 1 record
 * 
 *     Examples:
 *     DHL TTS 2.0.11, <NULL>, 10, version is complete, it took 10 statements to get there
 *     DHL TTS 2.0.11, DHL TTS 2.0.12, 4, patch is not complete, 4 statements already executed 
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.lcmg.rbloois.SystemException;
import com.lcmg.rbloois.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:17:41 PM
 */
public class DBVersion
{
	static protected boolean read; // read tried
	static protected boolean valid; // read succeeded
	static protected boolean tableexists;
	
	static protected String version;
	static protected String target;
	static protected int statements;
	static protected String user;
	
	static protected String getVersion()
	{
		if( !read )
			read();
		
		Assert.check( valid );
		
		if( !tableexists )
			return null;
		
		return version;
	}
	
	static protected String getTarget()
	{
		if( !read )
			read();
		
		Assert.check( valid );
		
		if( !tableexists )
			return null;
		
		return target;
	}
	
	static protected int getStatements()
	{
		if( !read )
			read();
		
		Assert.check( valid );
		
		if( !tableexists )
			return 0;
		
		return statements;
	}
	
	static protected void read()
	{
		Assert.notNull( user, "User is not set" );
		read = true;
		
		try
		{
			PreparedStatement statement = Database.getConnection( user ).prepareStatement( "SELECT VERSION, TARGET, STATEMENTS FROM DBVERSION" );
			ResultSet resultSet = statement.executeQuery();
			try
			{
				tableexists = true;
				
				Assert.check( resultSet.next() );
				version = resultSet.getString( 1 );
				target = resultSet.getString( 2 );
				statements = resultSet.getInt( 3 );
				Assert.check( !resultSet.next() );
				
				valid = true;
			}
			finally
			{
				statement.close();
			}
		}
		catch( SQLException e )
		{
			String sqlState = e.getSQLState();
			if( sqlState.equals( "42000" ) /* Oracle */ 
					|| sqlState.equals( "42X05" ) /* Derby */ )
			{
				valid = true;
				return;
			}
			
			throw new SystemException( e );
		}
	}
	
	static protected boolean doesTableExist()
	{
		if( !read )
			read();
		
		return tableexists;
	}
	
	static protected void setCount( String target, int statements )
	{
		Assert.check( tableexists, "Version tables do not exist" );
		
		try
		{
			PreparedStatement statement = Database.getConnection( user ).prepareStatement( "UPDATE DBVERSION SET TARGET = ?, STATEMENTS = ?" );
			statement.setString( 1, target );
			statement.setInt( 2, statements );
			int modified = statement.executeUpdate(); // autocommit is on
			Assert.check( modified == 1, "Expecting 1 record to be updated, not " + modified );
			statement.close();
			
			DBVersion.target = target;
			DBVersion.statements = statements;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	static protected void setVersion( String version )
	{
		Assert.check( tableexists, "Version tables do not exist" );
		
		try
		{
			PreparedStatement statement = Database.getConnection( user ).prepareStatement( "UPDATE DBVERSION SET VERSION = ?, TARGET = NULL" );
			statement.setString( 1, version );
			int modified = statement.executeUpdate(); // autocommit is on
			Assert.check( modified == 1, "Expecting 1 record to be updated, not " + modified );
			statement.close();
			
			DBVersion.version = version;
			DBVersion.target = null;
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}

	static protected void versionTablesCreated()
	{
		tableexists = true;
	}

	static protected void setUser( String user )
	{
		DBVersion.user = user;
	}

	static protected String getUser()
	{
		return user;
	}
}
