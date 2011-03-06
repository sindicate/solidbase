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

package solidbase.core;

import java.io.File;
import java.net.MalformedURLException;
import solidbase.util.ClassPathResource;
import solidbase.util.FileResource;
import solidbase.util.MemoryResource;
import solidbase.util.RandomAccessLineReader;
import solidbase.util.Resource;
import solidbase.util.URLRandomAccessLineReader;
import solidbase.util.URLResource;


/**
 * A factory to create some difficult data structures.
 * 
 * @author René M. de Bloois
 */
public final class Factory
{
	/**
	 * This utility class cannot be instantiated.
	 */
	private Factory()
	{
		super();
	}

	static public Resource getResource( String path )
	{
		return getResource( null, path );
	}

	static public Resource getResource( File baseDir, String path )
	{
		if( path.startsWith( "classpath:" ) )
			return new ClassPathResource( path );
		try
		{
			return new URLResource( path );
		}
		catch( MalformedURLException e )
		{
			return new FileResource( baseDir, path );
		}
	}

	/**
	 * Open the specified SQL file in the specified folder.
	 * 
	 * @param resource The resource to open.
	 * @param listener The progress listener.
	 * @return A random access reader for the file.
	 */
	static public RandomAccessLineReader openRALR( Resource resource, ProgressListener listener )
	{
		if( resource.supportsURL() ) // TODO supportsReopen()
		{
			listener.openingPatchFile( resource );
			return new URLRandomAccessLineReader( resource );
		}

		MemoryResource resource2 = new MemoryResource();
		resource2.readFromInputStream( resource.getInputStream() );
		return new URLRandomAccessLineReader( resource2 );
	}

	/**
	 * Open the specified SQL file in the specified folder.
	 *
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the SQL file.
	 * @param listener The progress listener.
	 * @return The SQL file.
	 */
	// TODO This should be done like openRALR
	static public SQLFile openSQLFile( Resource resource, ProgressListener listener )
	{
		listener.openingSQLFile( resource );
		SQLFile result = new SQLFile( resource );
		listener.openedSQLFile( result );
		return result;
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the upgrade file.
	 * @param listener The progress listener.
	 * @return The patch file.
	 */
	static public PatchFile openPatchFile( Resource resource, ProgressListener listener )
	{
		RandomAccessLineReader reader = openRALR( resource, listener );
		PatchFile result = new PatchFile( reader );
		try
		{
			result.scan();
		}
		catch( RuntimeException e )
		{
			// When read() fails, close the file.
			reader.close();
			throw e;
		}
		listener.openedPatchFile( result );
		return result;
	}
}
