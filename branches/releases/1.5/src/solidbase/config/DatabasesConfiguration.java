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

package solidbase.config;

import java.util.List;


/**
 * This class can be used in custom configuration. When the property <code>databases.config.class</code> is configured
 * in the properties file, this class will be used to configure the available databases. First
 * {@link #init(Configuration)} will be called to initialize the custom configuration, and after that
 * {@link #getDatabases()} will be called to retrieve the configured databases. When more then one databases are
 * configured in the command line version of SolidBase, the user will be presented a choice of databases to upgrade.
 * 
 * @author René M. de Bloois
 */
public interface DatabasesConfiguration
{
	/**
	 * Gets called first to initialize the custom configuration.
	 * 
	 * @param configuration The {@link Configuration} class that contains the configuration.
	 */
	void init( Configuration configuration );

	/**
	 * Returns the list of configured databases.
	 * 
	 * @return The list of configured databases.
	 */
	List< Database > getDatabases();
}
