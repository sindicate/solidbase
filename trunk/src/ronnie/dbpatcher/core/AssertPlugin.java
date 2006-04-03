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
	static Pattern pattern = Pattern.compile( "\\s*ASSERT\\s+EXISTS\\s+MESSAGE\\s+\"([^\"]*)\"\\s+(.*)", Pattern.DOTALL );
	
	public boolean execute( String sql ) throws SQLException
	{
		// TODO: case insensitivity
		
		Matcher matcher = pattern.matcher( sql );
		if( matcher.matches() )
		{
			String message = matcher.group( 1 );
			String select  = matcher.group( 2 ).trim();
			Assert.check( select.startsWith( "SELECT " ), "Check should be a SELECT" );
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
