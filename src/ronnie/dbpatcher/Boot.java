package ronnie.dbpatcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class Boot
{
	public static void main( String[] args )
	{
		try
		{
			Console.println( "Booting..." );

			// Get the current classloader
			URLClassLoader classLoader = (URLClassLoader)Boot.class.getClassLoader();

			// Add the driver jar(s) to the classpath
			URL[] orig = classLoader.getURLs();
			URL[] urls;
			if( Configuration.configVersion == 2 )
			{
				urls = new URL[ orig.length + Configuration.driverJars.size() ];
				System.arraycopy( orig, 0, urls, 0, orig.length );
				int i = orig.length;
				for( String driverJar : Configuration.driverJars )
				{
					File driverJarFile = new File( driverJar );
					urls[ i++ ] = driverJarFile.toURL();
					Console.println( "Adding jar to classpath: " + driverJarFile.toURL() );
				}
			}
			else
			{
				urls = new URL[ orig.length + 1 ];
				System.arraycopy( orig, 0, urls, 0, orig.length );
				String driverJar = Configuration.getDBDriverJar();
				File driverJarFile = new File( driverJar );
				urls[ urls.length - 1 ] = driverJarFile.toURL();
				Console.println( "Adding jar to classpath: " + driverJarFile.toURL() );
			}

			// Create a new classloader with the new classpath
			classLoader = new URLClassLoader( urls, Boot.class.getClassLoader().getParent() );

			// Execute the main class
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
