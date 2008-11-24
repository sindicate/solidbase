package ronnie.dbpatcher;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;

/**
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Main
{
	static protected void printCurrentVersion()
	{
		String version = Patcher.getCurrentVersion();
		String target = Patcher.getCurrentTarget();
		int statements = Patcher.getCurrentStatements();

		if( version == null )
		{
			if( target != null )
				Console.println( "The database has no version yet, incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
			else
				Console.println( "The database has no version yet." );
		}
		else
		{
			if( target != null )
			Console.println( "Current database version is \"" + version + "\", incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
		else
			Console.println( "Current database version is \"" + version + "\"." );
	}
	}

	static protected String list( List list )
	{
		StringBuffer buffer = new StringBuffer();

		boolean first = true;
		for( Iterator iter = list.iterator(); iter.hasNext(); )
		{
			Object object = iter.next();
			if( first )
				first = false;
			else
				buffer.append( ", " );
			buffer.append( object );
		}

		return buffer.toString();
	}

	static public void main( String[] args )
	{
		try
		{
			Console.println();
			Console.println( "DBPatcher v" + Configuration.getVersion() );
			Console.println( "(C) 2006-2008 R.M. de Bloois, LogicaCMG" );
			Console.println();

			boolean exportlog = false;
			boolean verbose = false;
			for( int i = 0; i < args.length; i++ )
			{
				String arg = args[ i ];
				if( args[ i ].equals( "exportlog" ) )
					exportlog = true;
				else if( args[ i ].equals( "verbose" ) )
					verbose = true;
				else if( args[ i ].equals( "fromant" ) )
					Console.fromAnt = true;
				else
					Assert.fail( "argument [" + arg + "] not recognized" );
			}

			Patcher.setCallBack( new Progress( verbose ) );

			if( Configuration.configVersion == 2 )
			{
				ronnie.dbpatcher.Configuration.Database selectedDatabase;
				if( Configuration.databases.size() > 1 )
				{
					Console.println( "Available database:" );
					for( ronnie.dbpatcher.Configuration.Database database : Configuration.databases )
						Console.println( "    " + database.name + " (" + database.description + ")" );
					Console.print( "Select a database from the above:" );
					String input = Console.input();
					selectedDatabase = Configuration.getDatabase( input );
					Console.println();
				}
				else
					selectedDatabase = Configuration.databases.get( 0 );

				ronnie.dbpatcher.Configuration.Application selectedApplication;
				if( selectedDatabase.applications.size() > 1 )
				{
					Console.println( "Available applications in database '" + selectedDatabase.description + "':" );
					for( ronnie.dbpatcher.Configuration.Application application : selectedDatabase.applications )
						Console.println( "    " + application.name + " (" + application.description + ")" );
					Console.print( "Select an application from the above:" );
					String input = Console.input();
					selectedApplication = selectedDatabase.getApplication( input );
					Console.println();
				}
				else
					selectedApplication = selectedDatabase.applications.get( 0 );

				Patcher.setConnection( new Database( selectedDatabase.driver, selectedDatabase.url ), selectedApplication.userName );
				Console.println( "Connecting to database '" + selectedDatabase.description + "', application '" + selectedApplication.description + "'..." );
			}
			else
			{
				Patcher.setConnection( new Database( Configuration.getDBDriver(), Configuration.getDBUrl() ), Configuration.getUser() );
				Console.println( "Connecting to database..." );
			}

			printCurrentVersion();

			if( exportlog )
			{
				Patcher.logToXML( System.out );
				return;
			}

			Patcher.openPatchFile();
			try
			{
				Patcher.readPatchFile();
				List targets = Patcher.getTargets();
				if( targets.size() > 0 )
				{
					Console.println( "Possible targets are: " + list( targets ) );
					Console.print( "Input target version: " );
					String input = Console.input();
					Patcher.patch( input );
					Console.emptyLine();
					printCurrentVersion();
				}
				else
					Console.println( "There are no possible targets." );
			}
			finally
			{
				Patcher.closePatchFile();
			}
		}
		catch( Exception e )
		{
			Console.println();
			if( e instanceof SystemException )
				if( e.getCause() != null )
					e = (Exception)e.getCause();
			if( e instanceof SQLException )
			{
				Console.println( "SQLState: " + ( (SQLException)e ).getSQLState() );
//				Console.println( "ErrorCode: " + ( (SQLException)e ).getErrorCode() );
			}
			e.printStackTrace( System.out );
		}
	}
}
