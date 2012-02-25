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

package solidbase.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


/**
 * A memory resource.
 *
 * @author Ren� M. de Bloois
 */
public class MemoryResource implements Resource
{
	/**
	 * The buffer containing the resource's bytes.
	 */
	protected List< byte[] > buffer = new LinkedList< byte[] >();

	/**
	 * Constructor for an empty memory resource.
	 */
	public MemoryResource()
	{
		// Default constructor
	}

	/**
	 * Constructs a new memory resource with the given bytes.
	 *
	 * @param bytes Bytes to use for the resource.
	 */
	public MemoryResource( byte[] bytes )
	{
		this.buffer.add( bytes );
	}

	/**
	 * Constructs a new memory resource by reading the input stream to the end.
	 *
	 * @param input The input stream to be read.
	 */
	public MemoryResource( InputStream input )
	{
		append( input );
	}

	public boolean supportsURL()
	{
		return false;
	}

	public URL getURL()
	{
		throw new UnsupportedOperationException();
	}

	public InputStream getInputStream()
	{
		return new ByteMatrixInputStream( this.buffer.toArray( new byte[ this.buffer.size() ][] ) );
	}

	public OutputStream getOutputStream()
	{
		// TODO Should we implement this?
		throw new UnsupportedOperationException();
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Appends the contents of the input stream to this memory resource.
	 *
	 * @param input The input stream to be read.
	 */
	public void append( InputStream input )
	{
		byte[] buffer = new byte[ 4096 ];
		int count;
		try
		{
			// FIXME Also handle the case where count == 0
			while( ( count = input.read( buffer ) ) > 0 )
			{
				byte[] b = new byte[ count ];
				System.arraycopy( buffer, 0, b, 0, count );
				this.buffer.add( b );
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	public String getPathFrom( Resource other )
	{
		throw new UnsupportedOperationException();
	}

	public boolean exists()
	{
		return true;
	}

	public long getLastModified()
	{
		return 0;
	}
}
