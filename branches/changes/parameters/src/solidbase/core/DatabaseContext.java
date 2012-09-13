/*--
 * Copyright 2011 René M. de Bloois
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Contains all configured database.
 *
 * @author René M. de Bloois
 * @since Aug 2011
 */
public class DatabaseContext
{
	/**
	 * The configured database.
	 */
	private Map< String, Database > databases = new HashMap< String, Database >();

	/**
	 * Constructor.
	 */
	public DatabaseContext()
	{
		//
	}

	/**
	 * Constructor.
	 *
	 * @param database A database.
	 */
	public DatabaseContext( Database database )
	{
		addDatabase( database );
	}

	/**
	 * Returns all configured databases.
	 *
	 * @return All configured databases.
	 */
	public Collection< Database > getDatabases()
	{
		return this.databases.values();
	}

	/**
	 * Adds a database.
	 *
	 * @param database A database.
	 */
	public void addDatabase( Database database )
	{
		this.databases.put( database.getName(), database );
	}

	/**
	 * Returns the database with the given name.
	 *
	 * @param name The name of the database.
	 * @return The database with the given name.
	 */
	public Database getDatabase( String name )
	{
		return this.databases.get( name );
	}
}
