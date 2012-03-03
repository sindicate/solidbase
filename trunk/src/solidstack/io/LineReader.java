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

package solidstack.io;


/**
 * A reader that reads lines or characters and maintains the current line number.
 *
 * @author René M. de Bloois
 */
public interface LineReader
{
	/**
	 * Close the reader.
	 */
	void close();

	/**
	 * Reads the next line. The current line number is incremented.
	 *
	 * @return The next line or null of there are no more lines to be read.
	 */
	String readLine();

	/**
	 * Returns the current line number. The current line number is the line that is about to be read with {@link #readLine()} or is being read with {@link #read()}.
	 *
	 * @return The current line number.
	 */
	int getLineNumber();

	/**
	 * Reads a character. Carriage return characters (\r) are filtered out in the following way:
	 * <ul>
	 * <li>\r\n becomes \n</li>
	 * <li>\r without \n becomes \n</li>
	 * </ul>
	 *
	 * @return A character. An \r is never returned.
	 */
	// TODO Decide if this javadoc is appropriate for this interface or should it be moved to the implementation?
	int read();

	/**
	 * Returns the underlying resource.
	 *
	 * @return The underlying resource.
	 */
	Resource getResource();

	/**
	 * Returns the current location.
	 *
	 * @return The current location.
	 * @see FileLocation
	 */
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
