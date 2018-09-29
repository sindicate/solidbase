/*--
 * Copyright 2010 Ren� M. de Bloois
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

package solidbase.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import solidstack.io.FatalIOException;


/**
 * Reads plugins from the classpath. First it collects all of the following files: META-INF/solidbase.plugins.
 * Each (non-empty) line in these files represents a plugin class. A plugin should extend {@link CommandListener}.
 *
 * @author Ren� M. de Bloois
 * @since May 2010
 */
// TODO Use ServiceManager to find the plugins
public class PluginManager
{
	/**
	 * All the plugins found.
	 */
	static protected List< CommandListener > listeners;

	static
	{
		listeners = new ArrayList<>();

		try
		{
			Enumeration< URL > resources = PluginManager.class.getClassLoader().getResources( "META-INF/solidbase.plugins" );
			while( resources.hasMoreElements() )
			{
				URL url = resources.nextElement();
				BufferedReader reader = new BufferedReader( new InputStreamReader( url.openStream() ) );
				try
				{
					String line = reader.readLine();
					while( line != null )
					{
						line = line.trim();
						if( line.length() > 0 )
						{
							Class< ? > pluginClass = Class.forName( line );
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
					reader.close();
				}
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
		catch( ClassNotFoundException e )
		{
			throw new FatalIOException( e );
		}
		catch( NoSuchMethodException e )
		{
			throw new FatalIOException( e );
		}
		catch( InstantiationException e )
		{
			throw new FatalIOException( e );
		}
		catch( IllegalAccessException e )
		{
			throw new FatalIOException( e );
		}
		catch( InvocationTargetException e )
		{
			throw new FatalIOException( e.getCause() );
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

	/**
	 * Send all listeners the signal to terminate
	 */
	static public void terminateListeners()
	{
		for( CommandListener listener : listeners )
			listener.terminate();
	}
}
