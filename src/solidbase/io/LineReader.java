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

package solidbase.io;


/**
 * A reader that reads lines or characters and maintains the current line number.
 *
 * @author Ren� M. de Bloois
 */
public interface LineReader
{
	/**
	 * Close the reader.
	 */
	void close();

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
	 * Reads a character. Must always be repeated until a \n is encountered, otherwise {@link #readLine()} will fail. An \r (carriage return) is never returned.
	 *
	 * @return A character. An \r is never returned.
	 */
	int read();

	/**
	 * Returns the underlying resource.
	 *
	 * @return The underlying resource.
	 */
	Resource getResource();

	FileLocation getLocation();

	/**
	 * Returns the character encoding of the source where the bytes are read from.
	 *
	 * @return The character encoding of the source where the bytes are read from.
	 */
	String getEncoding();

	/**
	 * Returns the BOM (Byte Order Mark) of the source where the bytes are read from.
	 *
	 * @return The BOM of the source where the bytes are read from.
	 */
	byte[] getBOM();
}
