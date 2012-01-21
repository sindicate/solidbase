/*--
 * Copyright 2011 René M. de Bloois
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

	// TODO Override the other read() methods too
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
