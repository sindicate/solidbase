package solidbase.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;



/**
 * Reads plugins from the classpath. First it collects all of the following files: META-INF/solidbase.plugins.
 * Each (non-empty) line in these files represents a plugin class. A plugin should extend {@link CommandListener}.
 * 
 * @author René M. de Bloois
 * @since May 2010
 */
public class PluginManager
{
	/**
	 * All the plugins found.
	 */
	static protected List< CommandListener > listeners;

	static
	{
		listeners = new ArrayList< CommandListener >();

		try
		{
			Enumeration< URL > resources = PluginManager.class.getClassLoader().getResources( "META-INF/solidbase.plugins" );
			while( resources.hasMoreElements() )
			{
				URL url = resources.nextElement();
				InputStream in = url.openStream();
				try
				{
					BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
					String line = reader.readLine();
					while( line != null )
					{
						line = line.trim();
						if( line.length() > 0 )
						{
							Class pluginClass = Class.forName( line );
							Object plugin = pluginClass.getConstructor().newInstance();
							if( !( plugin instanceof CommandListener ) )
								throw new FatalException( "Plugin class '" + line + "' should be extending " + CommandListener.class.getName() );
							listeners.add( (CommandListener)plugin );
						}

						line = reader.readLine();
					}
				}
				finally
				{
					in.close();
				}
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}
		catch( NoSuchMethodException e )
		{
			throw new SystemException( e );
		}
		catch( InstantiationException e )
		{
			throw new SystemException( e );
		}
		catch( IllegalAccessException e )
		{
			throw new SystemException( e );
		}
		catch( InvocationTargetException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Returns a list of all the plugins.
	 * 
	 * @return a list of all the plugins.
	 */
	static public List< CommandListener > getListeners()
	{
		return listeners;
	}
}
