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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import solidbase.core.Factory;
import solidbase.core.FatalException;

/**
 * A resource that is located in the classpath.
 *
 * @author René M. de Bloois
 */
public class ClassPathResource implements Resource
{
	/**
	 * The path of the resource.
	 */
	protected String path;

	/**
	 * Constructor.
	 *
	 * @param path The path of the resource.
	 */
	public ClassPathResource( String path )
	{
		if( path.startsWith( "classpath:" ) )
			this.path = path.substring( 10 );
		else
			this.path = path;
	}

	/**
	 * Always returns true.
	 */
	public boolean supportsURL()
	{
		return true;
	}

	/**
	 * Returns the URL for this resource.
	 */
	public URL getURL()
	{
		URL result = ClassPathResource.class.getClassLoader().getResource( this.path );
		if( result == null )
			// TODO Are we sure we want to throw this FatalException?
			throw new FatalException( "File " + toString() + " not found in classpath" );
		return result;
	}

	/**
	 * Returns an InputStream for this resource.
	 * @throws FileNotFoundException
	 */
	public InputStream getInputStream() throws FileNotFoundException
	{
		InputStream result = ClassPathResource.class.getClassLoader().getResourceAsStream( this.path );
		if( result == null )
			throw new FileNotFoundException( "File " + toString() + " not found in classpath" );
		return result;
	}

	/**
	 * Returns an OutputStream for this resource.
	 */
	public OutputStream getOutputStream()
	{
		throw new UnsupportedOperationException();
	}

	// TODO Need test for this
	public Resource createRelative( String path )
	{
		String scheme = URLResource.getScheme( path );
		if( scheme == null || scheme.equals( "classpath" ) )
		{
			if( scheme != null )
				path = path.substring( 10 );
			return new ClassPathResource( new File( new File( this.path ).getParentFile(), path ).getPath() );
		}
		return Factory.getResource( path );
	}

	@Override
	public String toString()
	{
		return this.path;
	}

	public String getPathFrom( Resource other )
	{
		throw new UnsupportedOperationException();
	}
}
