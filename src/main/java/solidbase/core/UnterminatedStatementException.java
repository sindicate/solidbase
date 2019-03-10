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

import solidstack.io.SourceException;
import solidstack.io.SourceLocation;


/**
 * Exception which is thrown when a statement is not terminated with one of the configured terminators.
 *
 * @author René M. de Bloois
 */
public class UnterminatedStatementException extends SourceException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param location The file location where the problem is located.
	 */
	public UnterminatedStatementException( SourceLocation location )
	{
		super( "Unterminated statement found", location );
	}
}
