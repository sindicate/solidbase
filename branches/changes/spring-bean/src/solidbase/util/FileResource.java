package solidbase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import solidbase.core.SystemException;

/**
 * A file resource.
 *
 * @author René M. de Bloois
 */
public class FileResource implements Resource
{
	/**
	 * The file.
	 */
	protected File file;

	/**
	 * Constructor.
	 *
	 * @param file The file. The file cannot be a directory.
	 */
	public FileResource( File file )
	{
		Assert.isFalse( file.isDirectory(), "File can't be a directory" );
		this.file = file;
	}

	/**
	 * Constructor.
	 *
	 * @param path The path of the file. The file cannot be a directory.
	 */
	public FileResource( String path )
	{
		this( new File( path ) );
	}

	/**
	 * Constructor for a relative file resource.
	 *
	 * @param parent The parent folder.
	 * @param path The path of the resource.
	 */
	public FileResource( File parent, String path )
	{
		this( new File( parent, path ) );
	}

	public boolean supportsURL()
	{
		return true;
	}

	public URL getURL()
	{
		try
		{
			return this.file.toURI().toURL();
		}
		catch( MalformedURLException e )
		{
			throw new SystemException( e ); // Not expected
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

	@Override
	public String toString()
	{
		return this.file.getAbsolutePath();
	}
}
