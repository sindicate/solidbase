/*--
 * Copyright 2009 René M. de Bloois
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

import static solidbase.util.Nulls.nonNull;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import solidbase.core.SystemException;
import solidstack.io.FatalIOException;


/**
 * Represents the version of SolidBase. It reads from version.properties.
 *
 * @author René M. de Bloois
 */
public class Version
{
	static private final String SOLIDBASE_VERSION_PROPERTIES = "version.properties";

	/**
	 * The version of SolidBase.
	 */
	static protected String version;

	static {
		// Load the solidbase version

		URL url = Version.class.getResource( SOLIDBASE_VERSION_PROPERTIES );
		if( url == null ) {
			throw new SystemException( "Can't find the " + SOLIDBASE_VERSION_PROPERTIES + " on the classpath" );
		}
		Properties properties = new Properties();
		try {
			properties.load( url.openStream() );
		} catch( IOException e ) {
			throw new FatalIOException( e );
		}
		version = nonNull( properties.getProperty( "solidbase.version" ) );
	}

	/**
	 * This class cannot be constructed.
	 */
	private Version() {
		super();
	}

	/**
	 * Get the SolidBase version &amp; copyright info to be displayed to the user.
	 *
	 * @return The SolidBase version &amp; copyright info to be displayed to the user.
	 */
	static public String getInfo() {
		return "SolidBase v" + version + " (http://solidbase.org)";
	}

}
