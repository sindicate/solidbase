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

import java.io.IOException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;


/**
 * This plugin will print the results from the SELECT to the console.
 *
 * <blockquote><pre>
 * PRINT SELECT 'Inserted ' || COUNT(*) || ' records in ATABLE.'
 * FROM ATABLE
 * GO
 * </pre></blockquote>
 *
 * @author René M. de Bloois
 * @since May 2010
 */
public class PrintSelect implements CommandListener
{
	static private final Pattern printSelectPattern = Pattern.compile( "PRINT\\s+(SELECT\\s+.+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	//@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = printSelectPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		String sql = matcher.group( 1 );

		Statement statement = processor.createStatement();
		try
		{
			ResultSet result = statement.executeQuery( sql );
			ResultSetMetaData metaData = result.getMetaData();
			int type = metaData.getColumnType( 1 );
			if( type == Types.CLOB || type == Types.NCLOB || type == Types.LONGVARCHAR || type == Types.LONGNVARCHAR )
			{
				StringBuilder buffer = new StringBuilder();
				char[] buf = new char[ 4096 ];
				while( result.next() )
				{
					Reader in = result.getCharacterStream( 1 );
					buffer.setLength( 0 );
					try
					{
						// TODO Can we do this in a streaming way? Maybe add streaming capability indicator to the output.
						for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
							buffer.append( buf, 0, read );
					}
					catch( IOException e )
					{
						throw new SQLException( e );
					}
					processor.getCallBack().print( buffer.toString() );
				}
			}
			else
			{
				while( result.next() )
					processor.getCallBack().print( result.getObject( 1 ).toString() );
			}
		}
		finally
		{
			processor.closeStatement( statement, true );
		}

		return true;
	}

	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}
}
