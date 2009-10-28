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

import java.util.ArrayList;
import java.util.List;

import solidbase.core.SystemException;


public class Database
{
	protected String name;
	protected String description;
	protected String driver;
	protected String url;
	protected List< Application > applications = new ArrayList();

	public Database( String name, String description, String driver, String url )
	{
		this.name = name;
		this.description = description;
		this.driver = driver;
		this.url = url;
	}

	public void addApplication( String name, String description, String userName, String patchFile )
	{
		this.applications.add( new Application( name, description, userName, patchFile ) );
	}

	public Application getApplication( String name )
	{
		for( Application application : this.applications )
			if( application.name.equals( name ) )
				return application;
		throw new SystemException( "Application [" + name + "] not configured for database [" + this.name + "]." );
	}

	public String getName()
	{
		return this.name;
	}

	public String getDescription()
	{
		return this.description;
	}

	public String getDriver()
	{
		return this.driver;
	}

	public String getUrl()
	{
		return this.url;
	}

	public List< Application > getApplications()
	{
		return this.applications;
	}

	static public class Comparator implements java.util.Comparator< Database >
	{
		public int compare( Database database1, Database database2 )
		{
			return database1.name.compareTo( database2.name );
		}
	}
}
