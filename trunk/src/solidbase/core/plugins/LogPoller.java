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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.ProgressListener;
import solidbase.core.SystemException;
import solidbase.util.Assert;
import solidstack.lang.ThreadInterrupted;


/**
 * The purpose of this plugin was to capture dbms output and present it to the user while the upgrade was running. It doesn't work however because
 * parallel statements through the same connection is not possible with the Oracle jdbc driver (or any driver).
 *
 * But this plugin acts as a good example for a future plugin that may implement the required functionality in a temporary table.
 *
 * @author René M. de Bloois
 * @since May 29, 2006
 */
public class LogPoller implements CommandListener
{
	static private Pattern disablePattern = Pattern.compile( "LOG\\s+POLLER\\s+OFF", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	static private Pattern enablePattern = Pattern.compile( "LOG\\s+POLLER\\s+ON", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	private Poller poller;

	//@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isPersistent() )
			return false;

		Matcher matcher = enablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = processor.getCurrentDatabase().newConnection();

			Assert.isNull( this.poller );
			this.poller = new Poller( processor.getProgressListener(), connection );
			this.poller.start();
			return true;
		}

		matcher = disablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			terminate();
			return true;
		}

		return false;
	}

	//@Override
	public void terminate()
	{
		if( this.poller != null )
		{
			Poller poller = this.poller;
			this.poller = null;

			poller.interrupt();
			try
			{
				poller.join();
			}
			catch( InterruptedException e )
			{
				throw new ThreadInterrupted();
			}
		}
	}

	static private class Poller extends Thread
	{
		private ProgressListener listener;
		private Connection connection;
		private int lastId;

		public Poller( ProgressListener listener, Connection connection )
		{
			this.listener = listener;
			this.connection = connection;
		}

		@Override
		public void run()
		{
			try
			{
				try
				{
					String sql = "SELECT ID, MESSAGE FROM LOG WHERE ID > ? ORDER BY ID";
					PreparedStatement statement = this.connection.prepareStatement( sql );

					while( !interrupted() )
					{
						statement.setInt( 1, this.lastId );
						ResultSet result = statement.executeQuery();
						while( result.next() )
						{
							this.listener.println( result.getString( 2 ) );
							this.lastId = result.getInt( 1 );
						}

						try
						{
							sleep( 200 );
						}
						catch( InterruptedException e )
						{
							throw new ThreadInterrupted();
						}
					}
				}
				finally
				{
					this.connection.close();
				}
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
	}
}
