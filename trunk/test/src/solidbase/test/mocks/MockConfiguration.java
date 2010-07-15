/*--
 * Copyright 2008 René M. de Bloois
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

package solidbase.test.mocks;

import java.io.File;

import mockit.Mock;


/**
 * A configuration stub used during testing.
 * 
 * @author René M. de Bloois
 * @since Dec 13, 2008
 */
public class MockConfiguration
{
	/**
	 * Path of the properties file.
	 */
	protected String propertyFileName;

	/**
	 * Constructor.
	 * 
	 * @param propertyFileName Path of the properties file.
	 */
	protected MockConfiguration( String propertyFileName )
	{
		this.propertyFileName = propertyFileName;
	}

	/**
	 * Returns the path of the properties file.
	 * 
	 * @return The path of the properties file.
	 */
	@Mock
	public File getPropertiesFile()
	{
		return new File( this.propertyFileName );
	}
}
