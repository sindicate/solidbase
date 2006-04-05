package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cmg.pas.util.Assert;

public class AssertPlugin extends Plugin
{
	static protected Pattern assertPattern = Pattern.compile( "\\s*ASSERT\\s+EXISTS\\s+MESSAGE\\s+\"([^\"]*)\"\\s+(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	
	public boolean execute( String sql ) throws SQLException
	{
		Matcher matcher = assertPattern.matcher( sql );
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
