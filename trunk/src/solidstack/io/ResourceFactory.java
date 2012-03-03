/*--
 * Copyright 2009 René M. de Bloois
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
import java.net.MalformedURLException;
import java.net.URL;


/**
 * A factory to create some difficult data structures.
 *
 * @author René M. de Bloois
 */
public final class ResourceFactory
{
	/**
	 * This utility class cannot be instantiated.
	 */
	private ResourceFactory()
	{
		super();
	}

	/**
	 * Creates a resource for the given path. If the path starts with classpath:, a {@link ClassPathResource} will be
	 * returned. If the path is a URL, a {@link URLResource} will be returned. Otherwise a {@link FileResource} is
	 * returned.
	 *
	 * @param path The path for the resource.
	 * @return The resource.
	 */
	static public Resource getResource( String path )
	{
		return getResource( null, path );
	}

	/**
	 * Creates a resource for the given path. If the path starts with classpath:, a {@link ClassPathResource}, {@link URLResource} or {@link FileResource} will be
	 * returned. If the path is a URL, a {@link URLResource} will be returned. Otherwise a {@link FileResource} is
	 * returned. The parent argument is only used when the path is not a URL (including the classpath protocol).
	 *
	 * @param parent The parent folder of the resource.
	 * @param path The path for the resource.
	 * @return The resource.
	 */
	static public Resource getResource( File parent, String path )
	{
		if( path.equals( "-" ) )
			return new SystemInOutResource();

		if( path.startsWith( "classpath:" ) )
		{
			Resource result = new ClassPathResource( path );
			try
			{
				URL url = result.getURL();
				if( url.getProtocol().equals( "jar" ) )
					return result;
				path = url.toString();
			}
			catch( FileNotFoundException e )
			{
				return result;
			}
		}

		try
		{

			Resource resource = new URLResource( path );
			URL url;
			try
			{
				url = resource.getURL();
				if( url.getProtocol().equals( "file" ) )
					return new FileResource( url.getFile() ); // TODO Check that the file is not a folder
			}
			catch( FileNotFoundException e )
			{
				// Ignore
			}
			return resource;
		}
		catch( MalformedURLException e )
		{
			return new FileResource( parent, path ); // TODO Check that the file is not a folder
		}
	}

	static public Resource getFolderResource( String path )
	{
		return getFolderResource( null, path );
	}

	/**
	 * Creates a resource for the given path. If the path starts with classpath:, a {@link ClassPathResource}, {@link URLResource} or {@link FileResource} will be
	 * returned. If the path is a URL, a {@link URLResource} will be returned. Otherwise a {@link FileResource} is
	 * returned. The parent argument is only used when the path is not a URL (including the classpath protocol).
	 *
	 * @param parent The parent folder of the resource.
	 * @param path The path for the resource.
	 * @return The resource.
	 */
	static public Resource getFolderResource( File parent, String path )
	{
		if( path.equals( "-" ) )
			throw new FatalIOException( "'-' not supported for folder resources" );

		if( path.startsWith( "classpath:" ) )
			return new ClassPathResource( path, true );

		try
		{
			Resource resource = new URLResource( path, true );
			URL url;
			try
			{
				url = resource.getURL();
				if( url.getProtocol().equals( "file" ) )
					return new FileResource( url.getFile() ); // TODO Check that the file is indeed a folder
			}
			catch( FileNotFoundException e )
			{
				// Ignore
			}
			return resource;
		}
		catch( MalformedURLException e )
		{
			return new FileResource( parent, path ); // TODO Check that the file is indeed a folder
		}
	}

	static public Resource currentFolder()
	{
		return getFolderResource( "" );
	}
}
