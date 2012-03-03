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
import solidbase.core.FatalException;
import solidbase.core.Runner;
import solidbase.core.SQLExecutionException;
import solidbase.core.SystemException;
import solidbase.util.Assert;
import solidstack.io.ResourceFactory;


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
		options.addOption( "dumplog", true, "export historical upgrade results to an XML file" );
		options.addOption( "driver", true, "sets the JDBC driverclass" );
		options.addOption( "url", true, "sets the URL for the database" );
		options.addOption( "username", true, "sets the default user name to connect with" );
		options.addOption( "password", true, "sets the password of the default user" );
		options.addOption( "target", true, "sets the target version to upgrade to" );
		options.addOption( "upgradefile", true, "specifies the file containing the database upgrades" );
		options.addOption( "sqlfile", true, "specifies an SQL file to execute" );
		options.addOption( "config", true, "specifies a properties file to use" );
		options.addOption( "downgradeallowed", false, "allow downgrades to reach the target" );
		options.addOption( "help", false, "Brings up this page" );

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

		solidbase.config.Options opts = new solidbase.config.Options( line.hasOption( "verbose" ), line
				.hasOption( "dumplog" ), line.getOptionValue( "driver" ), line.getOptionValue( "url" ), line
				.getOptionValue( "username" ), line.getOptionValue( "password" ), line.getOptionValue( "target" ), line
				.getOptionValue( "upgradefile" ), line.getOptionValue( "sqlfile" ), line.getOptionValue( "config" ),
				line.hasOption( "downgradeallowed" ), line.hasOption( "help" ) );

		if( opts.help )
		{
			printHelp( options );
			return;
		}

		Progress progress = new Progress( console, opts.verbose );
		Configuration configuration = new Configuration( progress, pass, opts );

		if( pass == 1 )
		{
			reload( args, configuration.getDriverJars(), opts.verbose );
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

		solidbase.config.Database def = configuration.getDefaultDatabase();
		Runner runner = new Runner();
		runner.setProgressListener( progress );
		runner.setConnectionAttributes( "default", def.getDriver(), def.getUrl(), def.getUserName(), def.getPassword() );
		for( solidbase.config.Database connection : configuration.getSecondaryDatabases() )
			runner.setConnectionAttributes(
					connection.getName(),
					connection.getDriver(),
					connection.getUrl(),
					connection.getUserName(),
					connection.getPassword()
					);

		if( configuration.getSqlFile() != null )
		{
			runner.setSQLFile( ResourceFactory.getResource( configuration.getSqlFile() ) );
			runner.executeSQL();
		}
		else if( opts.dumplog )
		{
			runner.setUpgradeFile( ResourceFactory.getResource( configuration.getUpgradeFile() ) );
			runner.setOutputFile( ResourceFactory.getResource( line.getOptionValue( "dumplog" ) ) );
			runner.logToXML();
		}
		else
		{
			runner.setUpgradeFile( ResourceFactory.getResource( configuration.getUpgradeFile() ) );
			runner.setUpgradeTarget( configuration.getTarget() );
			runner.setDowngradeAllowed( opts.downgradeallowed );
			runner.upgrade();
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
		Class< ? > main;
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
		// TODO Should we change the contextClassLoader too?
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
					Assert.notInstanceOf( t, SystemException.class );
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
