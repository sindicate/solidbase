package ronnie.dbpatcher.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lcmg.rbloois.SystemException;

/**
 * 
 * @author René M. de Bloois
 * @since May 29, 2006
 */
public class OracleDBMSOutputPlugin extends Plugin
{
	static protected Pattern disablePattern = Pattern.compile( "\\s*DISABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	static protected Pattern enablePattern = Pattern.compile( "\\s*ENABLE\\s+DBMSOUTPUT\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	
	protected Poller poller; 
	
	protected boolean execute( Command command ) throws SQLException
	{
		if( command.counting )
			return false;
		
		Matcher matcher = enablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = Database.getConnection();
			
			connection.prepareCall( "begin dbms_output.enable; end;" ).execute();
			System.out.println( "Enabled serveroutput" );

			this.poller = new Poller( connection );
			this.poller.start();
			return true;
		}
		
		matcher = disablePattern.matcher( command.getCommand() );
		if( matcher.matches() )
		{
			Connection connection = Database.getConnection();
			connection.prepareCall( "begin dbms_output.disable; end;" ).execute();
			System.out.println( "Disabled serveroutput" );

			terminate();
			return true;
		}
		
		return false;
	}
	
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
}

class Poller extends Thread
{
	protected Connection connection;
	
	protected Poller( Connection connection )
	{
		this.connection = connection;
	}

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
