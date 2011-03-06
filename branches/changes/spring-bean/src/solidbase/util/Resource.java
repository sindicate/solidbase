package solidbase.util;

import java.io.InputStream;
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
	 */
	URL getURL();

	/**
	 * Returns the input stream for this resource. Some resources allow only a single input stream to be retrieved.
	 *
	 * @return The input stream for this resource.
	 */
	InputStream getInputStream();

	/**
	 * Creates a resource with the given path relative to this resource.
	 *
	 * @param path The path of the resource.
	 * @return The new resource.
	 */
	Resource createRelative( String path );
}
