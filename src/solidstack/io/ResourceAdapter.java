package solidstack.io;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ResourceAdapter implements Resource
{
	protected boolean folder;

	public ResourceAdapter()
	{
	}

	public ResourceAdapter( boolean folder )
	{
		this.folder = folder;
	}

	public boolean supportsURL()
	{
		return false;
	}

	public boolean exists()
	{
		return true;
	}

	public long getLastModified()
	{
		return 0;
	}

	public boolean isFolder()
	{
		return this.folder;
	}

	public URL getURL() throws FileNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	public InputStream getInputStream() throws FileNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	public OutputStream getOutputStream()
	{
		throw new UnsupportedOperationException();
	}

	public Resource createRelative( String path )
	{
		throw new UnsupportedOperationException();
	}

	public String getPathFrom( Resource other )
	{
		throw new UnsupportedOperationException();
	}
}
