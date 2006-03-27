package ronnie.dbpatcher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

/*
 * TABLES:
 * 
 * DBVERSION ( VERSION, TARGET, STATEMENTS ), 1 record
 * 
 *     Examples:
 *     DHL TTS 2.0.11, <NULL>, 10, version is complete, it took 10 statements to get there
 *     DHL TTS 2.0.11, DHL TTS 2.0.12, 4, patch is not complete, 4 statements already executed 
 * 
 * DBVERSIONLOG ( VERSION, TARGET, STATEMENT, TIMESTAMP, SQL, RESULT )
 * 
 *     Version jumps:
 *     DHL TTS 2.0.11, <NULL>, <NULL>, 2006-03-27 13:56:00, <NULL>, <NULL>
 *     DHL TTS 2.0.12, <NULL>, <NULL>, 2006-03-27 13:56:00, <NULL>, <NULL>
 *     
 *     Individual statements:
 *     DHL TTS 2.0.11, DHL TTS 2.0.12, 5, 2006-03-27 13:56:00, CREATE TABLE ..., TABLE ALREADY EXISTS 
 */

public class DBVersion
{
//	static protected String tableName = "versioncontrol";
//	static protected String keyColumn = "vers_cont_id"; 
//	static protected String valueColumn = "vers_reg_num"; 
//	static protected String timestampColumn = "timestamp";
	
	static protected boolean read; // read tried
	static protected boolean valid; // read succeeded
	static protected boolean tableexists;
	
	static protected String version;
	
	static protected String getVersion()
	{
		if( !read )
			read();
		
		Assert.check( DBVersion.valid );
		
		if( !DBVersion.tableexists )
			return null;
		
		return DBVersion.version;
	}
	
	static protected void read()
	{
		DBVersion.read = true;
		
		try
		{
			PreparedStatement statement = Database.getConnection().prepareStatement( "SELECT VERSION FROM DBVERSION" );
			ResultSet resultSet = statement.executeQuery();
			try
			{
				DBVersion.tableexists = true;
				System.out.println( "table exists" );
				
				Assert.check( resultSet.next() );
				DBVersion.version = resultSet.getString( 1 );
				Assert.check( !resultSet.next() );
				
				DBVersion.valid = true;
			}
			finally
			{
				statement.close();
			}
		}
		catch( SQLException e )
		{
			String sqlState = e.getSQLState();
			if( sqlState.equals( "42000" ) || sqlState.equals( "42X05" ) ) // 42000: Oracle, 42X05: Derby
			{
				DBVersion.valid = true;
				return;
			}
			
//			System.out.println( e.getSQLState() );
			throw new SystemException( e );
		}
	}
	
//	static protected void createTables()
//	{
//		Assert.check( !DBVersion.tableexists );
//		try
//		{
//			Statement statement = Database.getConnection().createStatement();
////			statement.execute( "DROP TABLE DBVERSION" );
//			statement.execute( "CREATE TABLE DBVERSION ( VERSION VARCHAR(20) NOT NULL, TARGET VARCHAR(20), STATEMENTS INTEGER NOT NULL )" );
//			statement.execute( "CREATE TABLE DBVERSIONLOG ( VERSION VARCHAR(20) NOT NULL, TARGET VARCHAR(20) NOT NULL, STATEMENT INTEGER NOT NULL, TIMESTAMP TIMESTAMP NOT NULL, SQLSOURCE LONG VARCHAR NOT NULL, RESULT LONG VARCHAR )" );
//			statement.execute( "INSERT INTO DBVERSION ( VERSION, STATEMENTS ) VALUES ( '0', 0 )" );
//			statement.close();
//			
//			DBVersion.tableexists = true;
//		}
//		catch( SQLException e )
//		{
//			throw new SystemException( e );
//		}
//	}
	
	static protected boolean doesTableExist()
	{
		if( !read )
			read();
		
		return DBVersion.tableexists;
	}
}
