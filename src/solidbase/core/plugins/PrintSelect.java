/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.core.plugins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Assert;
import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.Database;


/**
 * This plugin executes PRINT SELECT statements.
 *
 * <blockquote><pre>
 * PRINT SELECT 'Inserted ' || COUNT(*) || ' users.'
 * FROM USERS
 * GO
 * </pre></blockquote>
 * 
 * This plugin will print the results from the SELECT to the console.
 *
 * @author René M. de Bloois
 * @since May 2010
 */
public class PrintSelect extends CommandListener
{
	static private final Pattern printSelectPattern = Pattern.compile( "PRINT\\s+(SELECT\\s+.+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	@Override
	protected boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = printSelectPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		String sql = matcher.group( 1 );

		Database database = processor.getCurrentDatabase();
		Connection connection = database.getConnection();
		PreparedStatement statement = null;
		try
		{
			Assert.isFalse( connection.getAutoCommit(), "Autocommit should be false" );
			statement = connection.prepareStatement( sql );
			ResultSet result = statement.executeQuery();
			while( result.next() )
			{
				Object object = result.getObject( 1 );
				processor.getCallBack().print( object.toString() );
			}
		}
		finally
		{
			if( statement != null )
				statement.close();
			connection.commit();
		}

		return true;
	}
}
