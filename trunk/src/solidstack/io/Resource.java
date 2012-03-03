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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * An abstraction of a resource. For example, a file, a URL, a resource in the classpath, or bytes in memory.
 *
 * @author René M. de Bloois
 */
public interface Resource
{
	/**
	 * Returns true if {@link #getURL()} is supported. False otherwise.
	 *
	 * @return True if and only if {@link #getURL()} is supported.
	 */
	boolean supportsURL();

	/**
	 * Returns the URL of the resource. Some resources throw an {@link UnsupportedOperationException}. Use
	 * {@link #supportsURL()} to determine if this call is supported or not.
	 *
	 * @return The URL of the resource.
	 * @throws FileNotFoundException
	 */
	URL getURL() throws FileNotFoundException;

	/**
	 * Returns the input stream for this resource. Some resources allow only a single input stream to be retrieved.
	 *
	 * @return The input stream for this resource.
	 * @throws FileNotFoundException
	 */
	InputStream getInputStream() throws FileNotFoundException;

	/**
	 * Returns the output stream for this resource. Some resources allow only a single output stream to be retrieved.
	 *
	 * @return The output stream for this resource.
	 */
	OutputStream getOutputStream();

	/**
	 * Creates a resource with the given path relative to this resource.
	 *
	 * @param path The path of the resource.
	 * @return The new resource.
	 */
	Resource createRelative( String path );

	String getPathFrom( Resource other );

	boolean exists();

	long getLastModified();

	boolean isFolder();
}
