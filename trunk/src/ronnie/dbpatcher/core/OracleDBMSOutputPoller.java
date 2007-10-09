package ronnie.dbpatcher.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.logicacmg.idt.commons.SystemException;

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
	static protected Pattern disablePattern = Pattern.compile( "\\s*DISABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	static protected Pattern enablePattern = Pattern.compile( "\\s*ENABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	
	protected Poller poller; 
	
	@Override
	protected boolean execute( Database database, Command command ) throws SQLException
	{
		if( command.isNonRepeatable() )
			return false;
		
		Matcher matcher = enablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = database.getConnection();
			
			connection.prepareCall( "begin dbms_output.enable; end;" ).execute();
			System.out.println( "Enabled serveroutput" );

			this.poller = new Poller( connection );
			this.poller.start();
			return true;
		}
		
		matcher = disablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = database.getConnection();
			connection.prepareCall( "begin dbms_output.disable; end;" ).execute();
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

	static protected class Poller extends Thread
	{
		protected Connection connection;
		
		protected Poller( Connection connection )
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
