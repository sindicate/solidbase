/*--
 * Copyright 2009 René M. de Bloois
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

package solidbase.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import solidbase.util.Assert;
import solidbase.util.RandomAccessLineReader;


/**
 * Some utilities.
 * 
 * @author René M. de Bloois
 */
public class Util
{
	/**
	 * This utility class cannot be constructed.
	 */
	private Util()
	{
		super();
	}

	/**
	 * Determines if the specified column is present in the resultset.
	 * 
	 * @param resultSet The resultset to check.
	 * @param columnName The column name to look for.
	 * @return True if the column is present in the resultset, false otherwise.
	 * @throws SQLException Can be thrown by JDBC.
	 */
	static public boolean hasColumn( ResultSet resultSet, String columnName ) throws SQLException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columns = metaData.getColumnCount();
		for( int i = 1; i <= columns; i++ )
			if( metaData.getColumnName( i ).equalsIgnoreCase( columnName ) )
				return true;
		return false;
	}

	/**
	 * Open the specified SQL file in the specified folder.
	 *
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the SQL file.
	 * @param listener The progress listener.
	 * @return A random access reader for the file.
	 */
	static public RandomAccessLineReader openRALR( File baseDir, String fileName, ProgressListener listener )
	{
		Assert.notNull( fileName );

		try
		{
			if( baseDir == null )
			{
				// TODO Should we remove this "/"?
				URL url = Util.class.getResource( "/" + fileName ); // In the classpath
				if( url != null )
				{
					listener.openingSQLFile( url );
					return new RandomAccessLineReader( url );
				}
			}

			File file = new File( baseDir, fileName ); // In the current folder
			listener.openingPatchFile( file );
			return new RandomAccessLineReader( file );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Open the specified SQL file in the specified folder.
	 *
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the SQL file.
	 * @param listener The progress listener.
	 * @return The SQL file.
	 */
	// TODO This should be done like openRALR
	static public SQLFile openSQLFile( File baseDir, String fileName, ProgressListener listener )
	{
		Assert.notNull( fileName );

		try
		{
			if( baseDir == null )
			{
				// TODO Should we remove this "/"?
				URL url = Util.class.getResource( "/" + fileName ); // In the classpath
				if( url != null )
				{
					listener.openingSQLFile( url );
					InputStream in = url.openStream();
					SQLFile result = new SQLFile( new BufferedInputStream( in ), url );
					listener.openedSQLFile( result );
					return result;
				}
			}

			File file = new File( baseDir, fileName ); // In the current folder
			listener.openingSQLFile( file );
			InputStream in = new FileInputStream( file );
			SQLFile result = new SQLFile( new BufferedInputStream( in ), file.toURI().toURL() );
			listener.openedSQLFile( result );
			return result;
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Open the specified SQL file.
	 *
	 * @param fileName The name and path of the SQL file.
	 * @param listener The progress listener.
	 * @return The SQL file.
	 */
	static public SQLFile openSQLFile( String fileName, ProgressListener listener )
	{
		return openSQLFile( null, fileName, listener );
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 * 
	 * @param baseDir The base folder from where to look. May be null.
	 * @param fileName The name and path of the upgrade file.
	 * @param listener The progress listener.
	 * @return The patch file.
	 */
	static public PatchFile openPatchFile( File baseDir, String fileName, ProgressListener listener )
	{
		if( fileName == null )
			fileName = "upgrade.sql";
		RandomAccessLineReader reader = openRALR( baseDir, fileName, listener );
		PatchFile result = new PatchFile( reader );
		try
		{
			result.scan();
		}
		catch( RuntimeException e )
		{
			// When read() fails, close the file.
			reader.close();
			throw e;
		}
		listener.openedPatchFile( result );
		return result;
	}

	/**
	 * Open the specified upgrade file in the specified folder.
	 * 
	 * @param fileName The name and path of the upgrade file.
	 * @param listener The progress listener.
	 * @return The patch file.
	 */
	static public PatchFile openPatchFile( String fileName, ProgressListener listener )
	{
		return openPatchFile( null, fileName, listener );
	}
}
