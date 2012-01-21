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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * An input stream resource.
 *
 * @author René M. de Bloois
 */
public class InputStreamResource implements Resource
{
	/**
	 * The input stream.
	 */
	protected InputStream inputStream;

	/**
	 * Constructor.
	 *
	 * @param inputStream The input stream.
	 */
	public InputStreamResource( InputStream inputStream )
	{
		if( inputStream == null )
			throw new IllegalArgumentException( "inputStream should not be null" );
		this.inputStream = inputStream;
	}

	public InputStream getInputStream()
	{
		if( this.inputStream == null )
			throw new IllegalStateException( "inputStream has been accessed earlier" );
		InputStream result = this.inputStream;
		this.inputStream = null;
		return result;
	}

	public OutputStream getOutputStream()
	{
		throw new UnsupportedOperationException();
	}

	public boolean supportsURL()
	{
		return false;
	}

	public URL getURL()
	{
		throw new UnsupportedOperationException();
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}
}
