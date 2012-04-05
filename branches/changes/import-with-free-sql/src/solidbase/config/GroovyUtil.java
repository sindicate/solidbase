/*--
 * Copyright 2006 Ren� M. de Bloois
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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import solidbase.core.SystemException;


/**
 * Groovy utilities.
 * 
 * @author Ren� M. de Bloois
 */
public class GroovyUtil
{
	/**
	 * This utility class cannot be constructed.
	 */
	private GroovyUtil()
	{
		super();
	}

	/**
	 * Execute the given groovy file using the specified binding.
	 * 
	 * @param file The groovy file.
	 * @param binding The binding to use.
	 * @return The return value of the groovy script.
	 */
	static public Object evaluate( File file, Map< String, ? > binding )
	{
		try
		{
			return new GroovyShell( new Binding( binding ) ).evaluate( file );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
