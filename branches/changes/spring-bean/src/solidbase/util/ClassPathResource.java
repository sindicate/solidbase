package solidbase.util;

import java.io.InputStream;
import java.net.URL;

import solidbase.core.FatalException;

public class ClassPathResource implements Resource
{
	protected String path;

	public ClassPathResource( String path )
	{
		if( path.startsWith( "classpath:" ) )
			this.path = path.substring( 10 );
		else
			this.path = path;
	}

	public boolean supportsURL()
	{
		return true;
	}

	public URL getURL()
	{
		URL result = ClassPathResource.class.getClassLoader().getResource( this.path );
		if( result == null )
			throw new FatalException( "File " + toString() + " not found in classpath" );
		return result;
	}

	public InputStream getInputStream()
	{
		InputStream result = ClassPathResource.class.getClassLoader().getResourceAsStream( this.path );
		if( result == null )
			throw new FatalException( "File " + toString() + " not found in classpath" );
		return result;
	}

	public Resource createRelative( String path )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString()
	{
		return this.path;
	}
}
