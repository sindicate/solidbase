package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cmg.pas.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class AssertPlugin extends Plugin
{
	static protected Pattern assertPattern = Pattern.compile( "\\s*ASSERT\\s+EXISTS\\s+MESSAGE\\s+\"([^\"]*)\"\\s+(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	
	public boolean execute( Command command ) throws SQLException
	{
		if( !command.counting )
			return false;
		
		
		Matcher matcher = assertPattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			String message = matcher.group( 1 );
			String select  = matcher.group( 2 ).trim();
			Assert.check( select.substring( 0, 7 ).equalsIgnoreCase( "SELECT " ), "Check should be a SELECT" );
			Connection connection = Database.getConnection();
			Statement statement = connection.createStatement();
			ResultSet result = statement.executeQuery( select );
			if( !result.next() )
				Assert.fail( message );
				
			return true;
		}
		return false;
	}
}
