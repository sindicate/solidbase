package ronnie.dbpatcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import ronnie.dbpatcher.config.Configuration;

public class Boot
{
	public static void main( String[] args )
	{
		Console console = new Console();

		try
		{
			boolean verbose = false;
			for( int i = 0; i < args.length; i++ )
				if( args[ i ].equals( "verbose" ) )
					verbose = true;

			if( verbose )
				console.println( "Booting..." );

			// Read the configuration files
			// TODO This is not needed when database is configured on the command line
			// TODO Merge the Boot and the Main class
			Configuration configuration = new Configuration( new Progress( console, false ), null, null, null, null, null );

			// Add the driver jar(s) to the classpath
			URLClassLoader classLoader = (URLClassLoader)Boot.class.getClassLoader();
			URL[] orig = classLoader.getURLs();
			URL[] urls;
			if( configuration.getConfigVersion() == 2 )
			{
				urls = new URL[ orig.length + configuration.getDriverJars().size() ];
				System.arraycopy( orig, 0, urls, 0, orig.length );
				int i = orig.length;
				for( String driverJar : configuration.getDriverJars() )
				{
					File driverJarFile = new File( driverJar );
					urls[ i++ ] = driverJarFile.toURL();
					if( verbose )
						console.println( "Adding jar to classpath: " + driverJarFile.toURL() );
				}
			}
			else
			{
				urls = new URL[ orig.length + 1 ];
				System.arraycopy( orig, 0, urls, 0, orig.length );
				String driverJar = configuration.getDBDriverJar();
				File driverJarFile = new File( driverJar );
				urls[ urls.length - 1 ] = driverJarFile.toURL();
				if( verbose )
					console.println( "Adding jar to classpath: " + driverJarFile.toURL() );
			}

			if( verbose )
				console.println();

			// Create a new classloader with the new classpath
			classLoader = new URLClassLoader( urls, Boot.class.getClassLoader().getParent() );

			// Execute the main class through the new classloader with reflection
			Class main = classLoader.loadClass( "ronnie.dbpatcher.Main" );
			Method method = main.getMethod( "main", new Class[] { String[].class } );
			method.invoke( method, new Object[] { args } );
		}
		catch( Throwable t )
		{
			t.printStackTrace();
		}
	}
}
