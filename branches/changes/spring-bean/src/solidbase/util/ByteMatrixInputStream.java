package solidbase.util;

import java.io.IOException;
import java.io.InputStream;

public class ByteMatrixInputStream extends InputStream
{
	protected byte[][] matrix;
	protected int pos1, pos2;

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
