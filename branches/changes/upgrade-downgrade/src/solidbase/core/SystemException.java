/*--
 * Copyright 2005 René M. de Bloois
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
 * Use this {@link RuntimeException} to wrap a checked exception that you can't handle and don't want to declare.
 * 
 * @author René M. de Bloois
 * @since Jan 8, 2005
 */
public class SystemException extends RuntimeException
{
	// This constructor should not be used
	protected SystemException()
	{
		super();
	}

	/**
	 * 
	 * @param message
	 */
	public SystemException( String message )
	{
		super( message );
	}

	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public SystemException( String message, Throwable cause )
	{
		super( message, cause );
	}

	/**
	 * 
	 * @param cause
	 */
	public SystemException( Throwable cause )
	{
		super( cause );
	}
}
