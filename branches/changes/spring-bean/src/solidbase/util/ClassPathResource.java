package solidbase.util;

import java.io.InputStream;
import java.net.URL;

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
	 */
	public InputStream getInputStream()
	{
		InputStream result = ClassPathResource.class.getClassLoader().getResourceAsStream( this.path );
		if( result == null )
			// TODO Are we sure we want to throw this FatalException?
			throw new FatalException( "File " + toString() + " not found in classpath" );
		return result;
	}

	/**
	 * Returns a new resource relative to this resource.
	 */
	public Resource createRelative( String path )
	{
		// TODO Implement
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString()
	{
		return this.path;
	}
}
