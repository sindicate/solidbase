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
	
			URLClassLoader classLoader = (URLClassLoader)Boot.class.getClassLoader();
			URL[] orig = classLoader.getURLs();
			URL[] urls = new URL[ orig.length + 1 ];
			System.arraycopy( orig, 0, urls, 0, orig.length );
			
			String driverJar = Configuration.getDBDriverJar();
			File driverJarFile = new File( driverJar );
			urls[ urls.length - 1 ] = driverJarFile.toURL();
			
			Console.println( "Adding jar to classpath: " + urls[ urls.length - 1 ] );

			classLoader = new URLClassLoader( urls, Boot.class.getClassLoader().getParent() );
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
