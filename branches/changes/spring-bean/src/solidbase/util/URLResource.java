package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import solidbase.core.SystemException;

public class URLResource implements Resource
{
	protected URL url;

	public URLResource( URL url )
	{
		this.url = url;
	}

	public URLResource( String url ) throws MalformedURLException
	{
		this( new URL( url ) );
	}

	public boolean supportsURL()
	{
		return true;
	}

	public URL getURL()
	{
		return this.url;
	}

	public InputStream getInputStream()
	{
		try
		{
			return this.url.openStream();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public Resource createRelative( String path )
	{
		try
		{
			return new URLResource( new URL( this.url, path ) );
		}
		catch( MalformedURLException e )
		{
			throw new SystemException( e );
		}
	}

	@Override
	public String toString()
	{
		return this.url.toString();
	}
}
