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

package solidbase.io;

import java.io.File;
import java.net.MalformedURLException;


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
	 * Creates a resource for the given path. If the path starts with classpath:, a {@link ClassPathResource} will be
	 * returned. If the path is a URL, a {@link URLResource} will be returned. Otherwise a {@link FileResource} is
	 * returned. The parent argument is only used when returning a {@link FileResource}.
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
			return new ClassPathResource( path );
		try
		{
			return new URLResource( path );
		}
		catch( MalformedURLException e )
		{
			return new FileResource( parent, path );
		}
	}
}
