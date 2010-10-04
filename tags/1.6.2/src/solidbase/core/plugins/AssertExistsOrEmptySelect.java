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

package solidbase.core.plugins;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Assert;
import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;


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
public class AssertExistsOrEmptySelect extends CommandListener
{
	static private final Pattern assertPattern = Pattern.compile( "\\s*ASSERT\\s+(EXISTS|EMPTY)\\s+MESSAGE\\s+['\"]([^']*)['\"]\\s+(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	/**
	 * Constructor.
	 */
	public AssertExistsOrEmptySelect()
	{
		super();
	}

	@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = assertPattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			String mode = matcher.group( 1 );
			String message = matcher.group( 2 );
			String select  = matcher.group( 3 ).trim();
			Assert.isTrue( select.substring( 0, 7 ).equalsIgnoreCase( "SELECT " ), "Check should be a SELECT" );
			Connection connection = processor.getCurrentDatabase().getConnection();
			Statement statement = connection.createStatement();
			try
			{
				boolean result = statement.executeQuery( select ).next();
				if( mode.equalsIgnoreCase( "EXISTS" ) ? !result : result )
					throw new CommandFileException( message, command.getLineNumber() );
				// Resultset is closed when the statement is closed
			}
			finally
			{
				// TODO The core engine should be able to check if a plugin leaves statements open.
				statement.close(); // Need to close the statement because the connection stays open.
				connection.commit();
			}

			return true;
		}
		return false;
	}
}
