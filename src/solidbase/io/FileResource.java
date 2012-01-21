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

package solidbase.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import solidbase.core.SystemException;
import solidbase.util.Assert;

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

	public InputStream getInputStream() throws FileNotFoundException
	{
		return new FileInputStream( this.file );
	}

	public OutputStream getOutputStream()
	{
		File parent = this.file.getParentFile();
		if( parent != null )
			parent.mkdirs();
		try
		{
			return new FileOutputStream( this.file );
		}
		catch( FileNotFoundException e )
		{
			throw new SystemException( e );
		}
	}

	// TODO Need test for this
	public Resource createRelative( String path )
	{
//		System.out.println( "Create relative [" + this.file + "] [" + path + "]" );
		String scheme = URLResource.getScheme( path );
		if( scheme == null || scheme.length() == 1 )
			return new FileResource( new File( this.file.getParentFile(), path ) );
		if( scheme.equals( "file" ) )
			return new URLResource( getURL() ).createRelative( path );
		return ResourceFactory.getResource( path );
	}

	// TODO Need test for this
	public String getPathFrom( Resource base )
	{
		Assert.isTrue( base instanceof FileResource );

		String myPath;
		String basePath;
		try
		{
			myPath = this.file.getCanonicalPath();
			basePath = ((FileResource)base).file.getCanonicalPath();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		// getCanonicalPath returns the os dependent path separator
		String[] myElems = myPath.split( "[\\\\/]" );
		String[] baseElems = basePath.split( "[\\\\/]" );

		int common = 0;
		while( common < myElems.length && common < baseElems.length && myElems[ common ].equals( baseElems[ common ] ) )
			common++;
		Assert.isTrue( common > 0 );

		StringBuffer result = new StringBuffer();

		if( baseElems.length > common )
			for( int j = 0; j < baseElems.length - common - 1; j++ )
				result.append( "../" );

		Assert.isTrue( common < myElems.length );
		result.append( myElems[ common ] );

		for( int j = common + 1; j < myElems.length; j++ )
		{
			result.append( '/' );
			result.append( myElems[ j ] );
		}

		return result.toString();
	}

	@Override
	public String toString()
	{
		return this.file.getAbsolutePath();
	}

	public boolean exists()
	{
		return this.file.exists();
	}

	public long getLastModified()
	{
		return this.file.lastModified();
	}
}
