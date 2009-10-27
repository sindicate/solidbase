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

public class Application
{
	protected String name;
	protected String description;
	protected String userName;
	protected String patchFile;

	protected Application( String name, String description, String userName, String patchFile )
	{
		this.name = name;
		this.description = description;
		this.userName = userName;
		this.patchFile = patchFile;
	}

	public String getName()
	{
		return this.name;
	}

	public String getDescription()
	{
		return this.description;
	}

	public String getUserName()
	{
		return this.userName;
	}

	public String getPatchFile()
	{
		return this.patchFile;
	}

	static public class Comparator implements java.util.Comparator< Application >
	{
		public int compare( Application application1, Application application2 )
		{
			return application1.name.compareTo( application2.name );
		}
	}
}
