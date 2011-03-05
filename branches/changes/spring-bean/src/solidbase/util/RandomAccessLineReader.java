package solidbase.util;

/**
 * A reader that reads lines or characters and has the ability to reposition itself on any give line number.
 * 
 * @author René M. de Bloois
 */
public interface RandomAccessLineReader extends LineReader
{
	/**
	 * Repositions the reader so that the given line number is the one that is to be read next.
	 * 
	 * @param lineNumber The number of the line that needs to be read next.
	 */
	void gotoLine( int lineNumber );
}
