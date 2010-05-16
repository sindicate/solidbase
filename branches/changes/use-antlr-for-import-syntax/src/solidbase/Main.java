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

package solidbase;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import solidbase.config.Configuration;
import solidbase.core.Assert;
import solidbase.core.Database;
import solidbase.core.FatalException;
import solidbase.core.PatchProcessor;
import solidbase.core.SQLExecutionException;
import solidbase.core.SystemException;


/**
 * This class contains the main method for the command line version of SolidBase.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Main
{
	/**
	 * The console for communication with the user.
	 */
	static public Console console;

	/**
	 * The current boot phase. Used to create a new class loader with an extended class path.
	 */
	static private int pass = 1;


	/**
	 * This class cannot be constructed.
	 */
	private Main()
	{
		super();
	}


	/**
	 * The main method for the command line version of SolidBase.
	 * 
	 * @param args The arguments from the command line.
	 */
	static public void main( String... args )
	{
		try
		{
			main0( args );
		}
		catch( Throwable t )
		{
			// Fancy stuff is done in pass2(). Checking type of exception does not work because of the new classloader we create.
			console.println();
			console.printStacktrace( t );
			System.exit( 1 );
		}
	}


	/**
	 * For internal (testing) use only: a main method that does not catch and print exceptions.
	 * 
	 * @param args The arguments from the command line.
	 * @throws SQLExecutionException When an {@link SQLException} is thrown during execution of a database change.
	 */
	// TODO Make this protected.
	static public void main0( String... args ) throws SQLExecutionException
	{
		if( console == null )
			console = new Console();

		// Configure the commandline options

		Options options = new Options();
		options.addOption( "verbose", false, "be extra verbose" );
//		options.addOption( "fromant", false, "adds newlines after input requests" );
		options.addOption( "dumplog", true, "export historical patch results to an xml file" );
		options.addOption( "driver", true, "sets the jdbc driverclass" );
		options.addOption( "url", true, "sets the url for the database" );
		options.addOption( "username", true, "sets the default username to patch with" );
		options.addOption( "password", true, "sets the password of the default username" );
		options.addOption( "target", true, "sets the target version" );
		options.addOption( "upgradefile", true, "specifies the file containing the database upgrades" );
		options.addOption( "config", true, "specifies the properties file to use" );
		options.addOption( "downgradeallowed", false, "allow downgrades to reach the target" );
		options.addOption( "help", false, "Brings up this page" );
		// TODO Add driverjar option

		options.getOption( "dumplog" ).setArgName( "filename" );
		options.getOption( "driver" ).setArgName( "classname" );
		options.getOption( "url" ).setArgName( "url" );
		options.getOption( "username" ).setArgName( "username" );
		options.getOption( "password" ).setArgName( "password" );
		options.getOption( "target" ).setArgName( "version" );
		options.getOption( "upgradefile" ).setArgName( "filename" );
		options.getOption( "config" ).setArgName( "filename" );

		// Read the commandline options

		CommandLine line;
		try
		{
			line = new GnuParser().parse( options, args );
		}
		catch( ParseException e )
		{
			console.println( e.getMessage() );
			printHelp( options );
			return;
		}

		boolean verbose = line.hasOption( "verbose" );
		boolean exportlog = line.hasOption( "dumplog" );
//		console.fromAnt = line.hasOption( "fromant" );
		boolean downgradeallowed = line.hasOption( "downgradeallowed" );
		boolean help = line.hasOption( "help" );

		if( help )
		{
			printHelp( options );
			return;
		}

		Progress progress = new Progress( console, verbose );
		Configuration configuration = new Configuration( progress, pass, line.getOptionValue( "driver" ), line.getOptionValue( "url" ), line.getOptionValue( "username" ), line.getOptionValue( "password" ), line.getOptionValue( "target" ), line.getOptionValue( "upgradefile" ), line.getOptionValue( "config" ) );

		if( pass == 1 )
		{
			reload( args, configuration.getDriverJars(), verbose );
			return;
		}

		String error = configuration.getFirstError();
		if( error != null )
		{
			console.println( error );
			printHelp( options );
			return;
		}

		if( configuration.isVoid() )
		{
			printHelp( options );
			return;
		}

		String[] info = Version.getInfo();
		console.println( info[ 0 ] );
		console.println( info[ 1 ] );
		console.println();

		PatchProcessor patcher = new PatchProcessor( progress );

		solidbase.config.Database defoult = configuration.getDefaultDatabase();
		patcher.addDatabase( "default", new Database( defoult.getDriver(), defoult.getUrl(), defoult.getUserName(), defoult.getPassword(), progress ) );

		for( solidbase.config.Database database : configuration.getSecondaryDatabases() )
			patcher.addDatabase( database.getName(),
					new Database( database.getDriver() == null ? defoult.getDriver() : database.getDriver(),
							database.getUrl() == null ? defoult.getUrl() : database.getUrl(),
									database.getUserName(), database.getPassword(), progress ) );

		patcher.init( configuration.getPatchFile() );
		try
		{
			if( exportlog )
			{
				patcher.logToXML( line.getOptionValue( "dumplog" ) );
				return;
			}

			console.println( "Connecting to database..." );
			console.println( patcher.getVersionStatement() );
			patcher.patch( configuration.getTarget(), downgradeallowed ); // TODO Print this target
			console.emptyLine();
			console.println( patcher.getVersionStatement() );
		}
		finally
		{
			patcher.end();
		}
	}


	/**
	 * Reload SolidBase with an extended classpath. Calls {@link #pass2(String...)} when it's done.
	 * 
	 * @param args The arguments from the command line.
	 * @param jars The jars that need to be added to the classpath.
	 * @param verbose Show more information.
	 * @throws SQLExecutionException When an {@link SQLException} is thrown during execution of a database change.
	 */
	static protected void reload( String[] args, List< String > jars, boolean verbose ) throws SQLExecutionException
	{
		if( jars == null || jars.isEmpty() )
		{
			// No need to add a new classloader
			pass2( args );
			return;
		}

		if( verbose )
			console.println( "Extending classpath" );

		// Add the driver jar(s) to the classpath
		URLClassLoader classLoader = (URLClassLoader)Main.class.getClassLoader();
		URL[] orig = classLoader.getURLs();
		URL[] urls;
		urls = new URL[ orig.length + jars.size() ];
		System.arraycopy( orig, 0, urls, 0, orig.length );
		int i = orig.length;
		for( String jar : jars )
		{
			File driverJarFile = new File( jar );
			try
			{
				urls[ i++ ] = driverJarFile.toURI().toURL();
			}
			catch( MalformedURLException e )
			{
				throw new SystemException( e );
			}
			if( verbose )
				console.println( "Adding jar to classpath: " + urls[ i - 1 ] );
		}

		if( verbose )
			console.println();

		// Create a new classloader with the new classpath
		classLoader = new URLClassLoader( urls, Main.class.getClassLoader().getParent() );

		// Execute the main class through the new classloader with reflection
		Class main;
		try
		{
			main = classLoader.loadClass( "solidbase.Main" );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}
		Method method;
		try
		{
			method = main.getDeclaredMethod( "pass2", String[].class );
		}
		catch( SecurityException e )
		{
			throw new SystemException( e );
		}
		catch( NoSuchMethodException e )
		{
			throw new SystemException( e );
		}
		try
		{
			method.invoke( method, (Object)args );
		}
		catch( IllegalArgumentException e )
		{
			throw new SystemException( e );
		}
		catch( IllegalAccessException e )
		{
			throw new SystemException( e );
		}
		catch( InvocationTargetException e )
		{
			throw new SystemException( e.getCause() );
		}
	}


	/**
	 * Gets called after reloading with an extended classpath.
	 * 
	 * @param args The arguments from the command line.
	 * @throws SQLExecutionException When an {@link SQLException} is thrown during execution of a database change.
	 */
	static public void pass2( String... args ) throws SQLExecutionException
	{
		try
		{
			pass = 2;
			main0( args );
		}
		catch( Throwable t )
		{
			console.println();

			if( t instanceof SystemException )
				if( t.getCause() != null )
				{
					t = t.getCause();
					Assert.isInstanceOf( t, SystemException.class );
				}

			if( t instanceof FatalException )
				console.println( "ERROR: " + t.getMessage() );
			else
				console.printStacktrace( t );

			System.exit( 1 );
		}
	}


	/**
	 * Print the help from commons cli to the writer registered on the {@link Console}.
	 * 
	 * @param options The commons cli option configuration.
	 */
	static protected void printHelp( Options options )
	{
		PrintWriter writer = new PrintWriter( console.out );
		new HelpFormatter().printHelp( writer, 80, "solidbase", null, options, 1, 3, null, true );
		writer.flush();
	}
}
