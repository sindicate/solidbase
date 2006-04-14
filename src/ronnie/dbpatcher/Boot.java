package ronnie.dbpatcher;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class Boot
{
	public static void main( String[] args )
	{
		try
		{
			System.out.println( "Booting..." );
	
			File dir = new File( "./" );
			dir = dir.getCanonicalFile();
			File[] jars = dir.listFiles
			( 
				new FilenameFilter()
				{
					public boolean accept( File dir, String name )
					{
						return name.endsWith( ".jar" );
					}
				}
			);
			
			URL[] urls = new URL[ jars.length ];
			for( int i = 0; i < jars.length; i++ )
			{
				urls[ i ] = jars[ i ].toURL();
				System.out.println( "Adding jar to classpath: " + urls[ i ] );
			}
			
//			MyClassLoader classLoader = new MyClassLoader( urls );
//			Class main = classLoader.myLoadClass( "ronnie.dbpatcher.Main" );
			
			URLClassLoader classLoader = new URLClassLoader( urls, Boot.class.getClassLoader().getParent() );
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
