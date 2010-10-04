/*--
 * Copyright 2010 Ren� M. de Bloois
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
 * A line reader.
 * 
 * @author Ren� M. de Bloois
 */
public interface LineReader
{
	/**
	 * Reads a line. The line number count is incremented.
	 * 
	 * @return The line that is read or null of there are no more lines.
	 */
	String readLine();

	/**
	 * Returns the current line number. The current line number is the line that is about to be read.
	 * 
	 * @return The current line number.
	 */
	int getLineNumber();

	/**
	 * Closes the reader and any underlying streams/readers.
	 */
	void close();
}
