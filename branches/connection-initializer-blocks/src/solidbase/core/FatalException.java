/*--
 * Copyright 2010 René M. de Bloois
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

/**
 * Represents an expected failure caused by a system check. As it is expected, a stacktrace is not required. The failure
 * message is enough to identify corrective actions.
 * 
 * @author René M. de Bloois
 */
public class FatalException extends RuntimeException
{
	/**
	 * Constructor.
	 * 
	 * @param message The failure message.
	 */
	public FatalException( String message )
	{
		super( message );
	}
}
