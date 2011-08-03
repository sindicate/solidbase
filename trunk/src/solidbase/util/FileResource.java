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

package solidbase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import solidbase.core.Factory;
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

	public OutputStream getOutputStream()
	{
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
		String scheme = URLResource.getScheme( path );
		if( scheme == null || scheme.length() == 1 )
			return new FileResource( new File( this.file.getParentFile(), path ) );
		if( scheme.equals( "file" ) )
			return new URLResource( getURL() ).createRelative( path );
		return Factory.getResource( path );
	}

	@Override
	public String toString()
	{
		return this.file.getAbsolutePath();
	}
}
