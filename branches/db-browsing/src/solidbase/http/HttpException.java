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

package solidbase.http;

/**
 * Use this exception for throwing system faults. System faults are not expected and are caused by programming bugs,
 * hardware malfunctions, configuration mistakes, missing files, unavailable servers, etc. The upstream code (the
 * callers of the method) have no interest in the condition that led to this exception being thrown.
 * 
 * @author René M. de Bloois
 * @since Nov 7, 2010
 */
public class HttpException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new system exception with the specified detail message.
	 * 
	 * @param message The detail message.
	 */
	public HttpException( String message )
	{
		super( message );
	}

	/**
	 * Constructs a new system exception with the specified detail message and cause.
	 * 
	 * @param message The detail message.
	 * @param cause The cause.
	 */
	public HttpException( String message, Throwable cause )
	{
		super( message, cause );
	}

	/**
	 * Constructs a new system exception with the specified cause.
	 * 
	 * @param cause The cause.
	 */
	public HttpException( Throwable cause )
	{
		super( cause );
	}
}
