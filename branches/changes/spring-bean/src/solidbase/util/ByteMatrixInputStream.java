package solidbase.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that reads from a 2-dimensional byte array.
 *
 * @author René M. de Bloois
 */
public class ByteMatrixInputStream extends InputStream
{
	/**
	 * The 2-dimensional byte array where the input stream reads its bytes from.
	 */
	protected byte[][] matrix;

	/**
	 * The position in the first dimension.
	 */
	protected int pos1;

	/**
	 * The position in the second dimension.
	 */
	protected int pos2;

	/**
	 * Constructs an input stream for the given 2-dimensional byte array.
	 *
	 * @param matrix The 2-dimensional byte array.
	 */
	public ByteMatrixInputStream( byte[][] matrix )
	{
		this.matrix = matrix;
	}

	@Override
	public int read() throws IOException
	{
		while( true )
		{
			if( this.pos1 >= this.matrix.length )
				return -1;
			byte[] buffer = this.matrix[ this.pos1 ];
			if( this.pos2 < buffer.length )
				return buffer[ this.pos2++ ];
			this.pos1++;
			this.pos2 = 0;
		}
	}
}
