package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.logicacmg.idt.commons.util.Assert;

/**
 * This plugin asserts that a given query statement returns results. It generates an error with the given message otherwise. This plugin can be used to check the state of the database. Example:
 *
 * <blockquote><pre>
 * ASSERT EXISTS MESSAGE 'Expecting old style version 2.0.17'
 * SELECT *
 * FROM VERSION_CONTROL
 * WHERE VERS_CONT_ID = 'VERSION'
 * AND VERS_REG_NUM = 'DHL TTS 2.0.17'
 * GO
 * </pre></blockquote>
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class AssertCommandExecuter extends CommandListener
{
	static private final Pattern assertPattern = Pattern.compile( "\\s*ASSERT\\s+EXISTS\\s+MESSAGE\\s+'([^']*)'\\s+(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	@Override
	protected boolean execute( Database database, Command command ) throws SQLException
	{
		if( command.isRepeatable() )
			return false;

		Matcher matcher = assertPattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			String message = matcher.group( 1 );
			String select  = matcher.group( 2 ).trim();
			Assert.isTrue( select.substring( 0, 7 ).equalsIgnoreCase( "SELECT " ), "Check should be a SELECT" );
			Connection connection = database.getConnection();
			Statement statement = connection.createStatement();
			try
			{
				ResultSet result = statement.executeQuery( select );
				if( !result.next() )
					Assert.fail( message );
			}
			finally
			{
				// TODO The core engine should be able to check if a plugin leaves statements open.
				statement.close(); // Need to close the statement because the connection stays open.
			}

			return true;
		}
		return false;
	}
}
