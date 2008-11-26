package ronnie.dbpatcher;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ronnie.dbpatcher.config.Configuration;
import ronnie.dbpatcher.core.Database;
import ronnie.dbpatcher.core.Patcher;

import com.logicacmg.idt.commons.SystemException;

/**
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Main
{
	static protected void printCurrentVersion( Console console )
	{
		String version = Patcher.getCurrentVersion();
		String target = Patcher.getCurrentTarget();
		int statements = Patcher.getCurrentStatements();

		if( version == null )
		{
			if( target != null )
				console.println( "The database has no version yet, incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
			else
				console.println( "The database has no version yet." );
		}
		else
		{
			if( target != null )
				console.println( "Current database version is \"" + version + "\", incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
			else
				console.println( "Current database version is \"" + version + "\"." );
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
		Console console = new Console();

		try
		{
			// Configure the commandline options

			Options options = new Options();
			options.addOption( "verbose", false, "be extra verbose" );
			options.addOption( "fromant", false, "adds newlines after input requests" );
			options.addOption( "export", false, "export historical patch results to an xml file" ); // TODO Add filename as an option value
			options.addOption( "driver", true, "sets the jdbc driverclass" );
			options.addOption( "url", true, "sets the url of the database" );
			options.addOption( "user", true, "sets the default username to patch with" );
			options.addOption( "pass", true, "sets the password of the default username" );
			options.addOption( "target", true, "sets the target version" );

			options.getOption( "driver" ).setArgName( "classname" );
			options.getOption( "url" ).setArgName( "url" );
			options.getOption( "user" ).setArgName( "username" );
			options.getOption( "pass" ).setArgName( "password" );
			options.getOption( "target" ).setArgName( "targetversion" );

			// Read the commandline options

			CommandLine line;
			try
			{
				line = new GnuParser().parse( options, args );
			}
			catch( ParseException e )
			{
				console.println( e.getMessage() );
				new HelpFormatter().printHelp( "dbpatcher", options, true );
				return;
			}

			boolean verbose = line.hasOption( "verbose" );
			boolean exportlog = line.hasOption( "exportlog" );
			console.fromAnt = line.hasOption( "fromant" );

			// Validate the commandline options

			if( line.hasOption( "driver" ) || line.hasOption( "url" ) || line.hasOption( "user" ) )
			{
				boolean valid = true;
				if( !line.hasOption( "driver" ) )
				{
					console.println( "Missing driver option" );
					valid = false;
				}
				if( !line.hasOption( "url" ) )
				{
					console.println( "Missing url option" );
					valid = false;
				}
				if( !line.hasOption( "user" ) )
				{
					console.println( "Missing user option" );
					valid = false;
				}
				if( !valid )
				{
					new HelpFormatter().printHelp( "dbpatcher", options, true );
					return;
				}
			}

			//

			Progress progress = new Progress( console, verbose );
			Configuration configuration = new Configuration( progress, line.getOptionValue( "driver" ), line.getOptionValue( "url" ), line.getOptionValue( "user" ), line.getOptionValue( "pass" ), line.getOptionValue( "target" ) );

			console.println( "DBPatcher v" + configuration.getVersion() );
			console.println( "(C) 2006-2008 R.M. de Bloois, LogicaCMG" );
			console.println();

			Patcher.setCallBack( progress );
			String patchFile;
			if( configuration.getConfigVersion() == 2 )
			{
				ronnie.dbpatcher.config.Configuration.Database selectedDatabase;
				if( configuration.getDatabases().size() > 1 )
				{
					console.println( "Available database:" );
					for( ronnie.dbpatcher.config.Configuration.Database database : configuration.getDatabases() )
						console.println( "    " + database.getName() + " (" + database.getDescription() + ")" );
					console.print( "Select a database from the above:" );
					String input = console.input();
					selectedDatabase = configuration.getDatabase( input );
					console.println();
				}
				else
					selectedDatabase = configuration.getDatabases().get( 0 );

				ronnie.dbpatcher.config.Configuration.Application selectedApplication;
				if( selectedDatabase.getApplications().size() > 1 )
				{
					console.println( "Available applications in database '" + selectedDatabase.getDescription() + "':" );
					for( ronnie.dbpatcher.config.Configuration.Application application : selectedDatabase.getApplications() )
						console.println( "    " + application.getName() + " (" + application.getDescription() + ")" );
					console.print( "Select an application from the above:" );
					String input = console.input();
					selectedApplication = selectedDatabase.getApplication( input );
					console.println();
				}
				else
					selectedApplication = selectedDatabase.getApplications().get( 0 );

				Patcher.setConnection( new Database( selectedDatabase.getDriver(), selectedDatabase.getUrl() ), selectedApplication.getUserName(), null );
				patchFile = selectedApplication.getPatchFile();
				console.println( "Connecting to database '" + selectedDatabase.getDescription() + "', application '" + selectedApplication.getDescription() + "'..." );
			}
			else
			{
				Patcher.setConnection( new Database( configuration.getDBDriver(), configuration.getDBUrl() ), configuration.getUser(), configuration.getPassWord() );
				patchFile = "dbpatch.sql";
				console.println( "Connecting to database..." );
			}

			printCurrentVersion( console );

			if( exportlog )
			{
				Patcher.logToXML( System.out );
				return;
			}

			Patcher.openPatchFile( patchFile );
			try
			{
				List targets = Patcher.getTargets();
				if( targets.size() > 0 )
				{
					console.println( "Possible targets are: " + list( targets ) );
					console.print( "Input target version: " );
					String input = console.input();
					Patcher.patch( input );
					console.emptyLine();
					printCurrentVersion( console );
				}
				else
					console.println( "There are no possible targets." );
			}
			finally
			{
				Patcher.closePatchFile();
			}
		}
		catch( Exception e )
		{
			console.println();

			if( e instanceof SystemException )
				if( e.getCause() != null )
					e = (Exception)e.getCause();

			if( e instanceof SQLException )
				console.println( "SQLState: " + ( (SQLException)e ).getSQLState() );

			e.printStackTrace( System.out );
		}
	}
}
