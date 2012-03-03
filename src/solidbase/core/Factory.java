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

import java.io.FileNotFoundException;

import solidstack.io.MemoryResource;
import solidstack.io.RandomAccessBOMDetectingLineReader;
import solidstack.io.RandomAccessLineReader;
import solidstack.io.Resource;


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

	/**
	 * Open the specified SQL file in the specified folder.
	 *
	 * @param resource The resource to open.
	 * @param listener The progress listener.
	 * @return A random access reader for the file.
	 */
	static public RandomAccessLineReader openRALR( Resource resource, ProgressListener listener )
	{
		try
		{
			// TODO supportsURL() is not right for this purpose.
			if( resource.supportsURL() )
			{
				listener.openingUpgradeFile( resource );
				return new RandomAccessBOMDetectingLineReader( resource );
			}

			// TODO What about the message? "Opening internal resource..."
			MemoryResource memResource = new MemoryResource();
			memResource.append( resource.getInputStream() );
			return new RandomAccessBOMDetectingLineReader( memResource );
		}
		catch( FileNotFoundException e )
		{
			throw new FatalException( e.toString() ); // TODO e or e.toString()
		}
	}

	/**
	 * Open the specified SQL file in the specified folder.
	 *
	 * @param resource The resource containing the SQL file.
	 * @param listener The progress listener.
	 * @return The SQL file.
	 */
	// TODO This should be done like openRALR
	static public SQLFile openSQLFile( Resource resource, ProgressListener listener )
	{
		// What if the resource does not have a URL?
		listener.openingSQLFile( resource );
		SQLFile result = new SQLFile( resource );
		listener.openedSQLFile( result );
		return result;
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 *
	 * @param resource The resource containing the upgrade file.
	 * @param listener The progress listener.
	 * @return The upgrade file.
	 */
	static public UpgradeFile openUpgradeFile( Resource resource, ProgressListener listener )
	{
		RandomAccessLineReader reader = openRALR( resource, listener );
		UpgradeFile result = new UpgradeFile( reader );
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
		listener.openedUpgradeFile( result );
		return result;
	}
}
