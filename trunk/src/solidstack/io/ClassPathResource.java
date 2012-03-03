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

package solidstack.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;


/**
 * A resource that is located in the classpath.
 *
 * @author René M. de Bloois
 */
public class ClassPathResource extends ResourceAdapter
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
		this( path, false );
	}

	public ClassPathResource( String path, boolean folder )
	{
		super( folder );
		if( path.startsWith( "classpath:" ) )
			this.path = path.substring( 10 );
		else
			this.path = path;
	}

	/**
	 * Always returns true.
	 */
	@Override
	public boolean supportsURL()
	{
		return true;
	}

	/**
	 * Returns the URL for this resource.
	 * 
	 * @throws FileNotFoundException When a file is not found.
	 */
	@Override
	public URL getURL() throws FileNotFoundException
	{
		URL result = ClassPathResource.class.getClassLoader().getResource( this.path );
		if( result == null )
			throw new FileNotFoundException( "File " + toString() + " not found in classpath" );
		return result;
	}

	/**
	 * Returns an InputStream for this resource.
	 * 
	 * @throws FileNotFoundException When a file is not found.
	 */
	@Override
	public InputStream getInputStream() throws FileNotFoundException
	{
		InputStream result = ClassPathResource.class.getClassLoader().getResourceAsStream( this.path );
		if( result == null )
			throw new FileNotFoundException( "File " + toString() + " not found in classpath" );
		return result;
	}

	// TODO Need test for this
	@Override
	public Resource createRelative( String path )
	{
		String scheme = URLResource.getScheme( path );
		if( scheme == null || scheme.equals( "classpath" ) )
		{
			if( scheme != null )
				path = path.substring( 10 );
			File parent = new File( this.path );
			if( !isFolder() )
				parent = parent.getParentFile();
			path = new File( parent, path ).getPath().replace( '\\', '/' ); // ClassLoader does not understand backslashes
			return ResourceFactory.getResource( "classpath:" + path );
		}
		return ResourceFactory.getResource( path );
	}

	@Override
	public String toString()
	{
		return this.path;
	}

	@Override
	public boolean exists()
	{
		return ClassPathResource.class.getClassLoader().getResource( this.path ) != null;
	}

	@Override
	public long getLastModified()
	{
		return 0;
	}
}
