/*--
 * Copyright 2009 Ren� M. de Bloois
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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import solidbase.core.Assert;
import solidbase.core.SystemException;


/**
 * 
 * @author Ren� M. de Bloois
 */
public class Version
{
	static private final String DBPATCHER_VERSION_PROPERTIES = "version.properties";

	/**
	 * The version of SolidBase.
	 */
	static protected String version;

	static
	{
		// Load the version properties

		URL url = Version.class.getResource( DBPATCHER_VERSION_PROPERTIES );
		Assert.notNull( url );
		Properties properties = new Properties();
		try
		{
			properties.load( url.openStream() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		version = properties.getProperty( "solidbase.version" );
		Assert.notNull( version );
	}

	/**
	 * Get the SolidBase version & copyright info to be displayed to the user.
	 * 
	 * @return The SolidBase version & copyright info to be displayed to the user.
	 */
	static public String[] getInfo()
	{
		// TODO Ant messes up the encoding, try add the � again
		return new String[] { "SolidBase v" + version, "(C) 2006-2010 Rene M. de Bloois" };
	}
}
