package solidbase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import solidbase.core.SystemException;

public class FileResource implements Resource
{
	protected File file;

	public FileResource( File file )
	{
		Assert.isFalse( file.isDirectory(), "File can't be a directory" );
		this.file = file;
	}

	public FileResource( String path )
	{
		this( new File( path ) );
	}

	public URL getURL()
	{
		try
		{
			return this.file.toURI().toURL();
		}
		catch( MalformedURLException e )
		{
			throw new SystemException( e );
		}
	}

	public InputStream getInputStream()
	{
		try
		{
			return new FileInputStream( this.file );
		}
		catch( FileNotFoundException e )
		{
			throw new SystemException( e );
		}
	}

	public Resource createRelative( String path )
	{
		return new FileResource( new File( this.file.getParentFile(), path ) );
	}
}
