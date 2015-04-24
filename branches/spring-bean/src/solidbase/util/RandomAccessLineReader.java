/*--
 * Copyright 2011 Ren� M. de Bloois
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

package solidbase.util;

/**
 * A reader that reads lines or characters and has the ability to reposition itself on any give line number.
 *
 * @author Ren� M. de Bloois
 */
public interface RandomAccessLineReader extends LineReader
{
	/**
	 * Repositions the reader so that the given line number is the one that is to be read next.
	 *
	 * @param lineNumber The number of the line that needs to be read next.
	 */
	void gotoLine( int lineNumber );

	/**
	 * Re-open the file with another encoding.
	 *
	 * @param encoding The new encoding.
	 */
	void reOpen( String encoding );
}
