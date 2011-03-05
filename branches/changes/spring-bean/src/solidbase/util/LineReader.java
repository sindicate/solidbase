package solidbase.util;

import java.net.URL;

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
	 * Returns the {@link URL} of the source where the lines are read from.
	 * 
	 * @return The {@link URL} of the source where the lines are read from.
	 */
	URL getURL();

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
