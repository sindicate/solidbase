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

package solidbase.core;


/**
 * Exception that is thrown when a statement is not terminated with a GO.
 * 
 * @author René M. de Bloois
 */
// TODO Maybe reuse FatalException
public class UnterminatedStatementException extends RuntimeException
{
	/**
	 * The line number of the unterminated statement.
	 */
	protected int lineNumber;

	/**
	 * Constructor.
	 * 
	 * @param lineNumber The line number of the unterminated statement.
	 */
	public UnterminatedStatementException( int lineNumber )
	{
		this.lineNumber = lineNumber;
	}

	@Override
	public String getMessage()
	{
		return "Unterminated statement found at line " + this.lineNumber;
	}
}
