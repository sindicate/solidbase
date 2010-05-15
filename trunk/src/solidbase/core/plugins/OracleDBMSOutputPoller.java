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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.SystemException;


/**
 * The purpose of this plugin was to capture dbms output and present it to the user while the patch was running. It doesn't work however because
 * parallel statements through the same connection is not possible with the Oracle jdbc driver (or any driver).
 * 
 * But this plugin acts as a good example for a future plugin that may implement the required functionality in a temporary table.
 * 
 * @author René M. de Bloois
 * @since May 29, 2006
 */
public class OracleDBMSOutputPoller extends CommandListener
{
	static private Pattern disablePattern = Pattern.compile( "\\s*DISABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	static private Pattern enablePattern = Pattern.compile( "\\s*ENABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	private Poller poller;

	/**
	 * Constructor.
	 */
	public OracleDBMSOutputPoller()
	{
		super();
	}

	@Override
	protected boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isPersistent() )
			return false;

		Matcher matcher = enablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = processor.getCurrentDatabase().getConnection();

			CallableStatement call = connection.prepareCall( "begin dbms_output.enable; end;" );
			try
			{
				call.execute();
			}
			finally
			{
				call.close();
			}
			System.out.println( "Enabled serveroutput" );

			this.poller = new Poller( connection );
			this.poller.start();
			return true;
		}

		matcher = disablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = processor.getCurrentDatabase().getConnection();
			CallableStatement call = connection.prepareCall( "begin dbms_output.disable; end;" );
			try
			{
				call.execute();
			}
			finally
			{
				call.close();
			}
			System.out.println( "Disabled serveroutput" );

			terminate();
			return true;
		}

		return false;
	}

	@Override
	protected void terminate()
	{
		if( this.poller != null )
		{
			this.poller.interrupt();
			try
			{
				this.poller.join();
			}
			catch( InterruptedException e )
			{
				throw new SystemException( e );
			}
		}
	}

	static private class Poller extends Thread
	{
		private Connection connection;

		Poller( Connection connection )
		{
			this.connection = connection;
		}

		@Override
		public void run()
		{
			try
			{
				System.out.println( "Started Poller thread" );

				String sql = "begin dbms_output.get_line( ?, ? ); end;";
				CallableStatement statement = this.connection.prepareCall( sql );
				statement.registerOutParameter( 1, Types.VARCHAR );
				statement.registerOutParameter( 2, Types.INTEGER );

				boolean stop = false;
				boolean needsleep = false;
				while( !stop && !isInterrupted() )
				{
					System.out.println( "Poll" );
					needsleep = true;

					statement.execute();
					while( statement.getInt( 2 ) == 0 )
					{
						System.out.println( statement.getString( 1 ) );
						statement.execute();
					}

					if( needsleep )
					{
						needsleep = false;
						try
						{
							sleep( 1000 );
						}
						catch( InterruptedException e )
						{
							stop = true;
						}
					}
				}

				this.connection.close();

				System.out.println( "Finished Poller thread" );
			}
			catch( Throwable t )
			{
				System.err.println( "The DBMSOutput poller thread crashed" );
				t.printStackTrace( System.err );
			}
		}
	}
}
