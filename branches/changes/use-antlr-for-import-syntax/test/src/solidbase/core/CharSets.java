/*--
 * Copyright 2006 René M. de Bloois
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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.PatchProcessor;
import solidbase.core.RandomAccessLineReader;
import solidbase.test.core.TestProgressListener;


public class CharSets
{
	@Test
	public void testIso8859() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "testpatch1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.read();
		Assert.assertEquals( patchFile.file.getEncoding(), "ISO-8859-1" );
		patchFile.close();
	}

	@Test
	public void testUtf8() throws SQLException, SQLExecutionException
	{
		TestProgressListener progress = new TestProgressListener();
		PatchProcessor patcher = new PatchProcessor( progress, new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testUtf8", "sa", null, progress ) );

		patcher.init( "patch-utf-8-1.sql" );
		Assert.assertEquals( patcher.patchFile.file.getEncoding(), "UTF-8" );
		try
		{
			patcher.patch( "1.0.2" );
		}
		finally
		{
			patcher.closePatchFile();
		}

		Connection connection = patcher.currentDatabase.getConnection( "sa" );
		Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM USERS" );
		assert result.next();
		String userName = result.getString( "USER_USERNAME" );
		Assert.assertEquals( userName, "rené" );
		patcher.end();
	}

	@Test
	public void testUtf16Bom() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "patch-utf-16-bom-1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.read();
		Assert.assertEquals( patchFile.file.getBOM(), new byte[] { -1, -2 } );
		Assert.assertEquals( patchFile.file.getEncoding(), "UTF-16LE" );
		patchFile.close();
	}

	@Test
	public void testUtf16BomAndExplicit() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "patch-utf-16-bom-2.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.read();
		Assert.assertEquals( patchFile.file.getBOM(), new byte[] { -1, -2 } );
		Assert.assertEquals( patchFile.file.getEncoding(), "UTF-16LE" );

		RandomAccessLineReader reader = patchFile.file;
		reader.gotoLine( 1 );
		boolean found = false;
		String line = reader.readLine();
		while( line != null )
		{
			if( line.contains( "rené" ) )
				found = true;
			line = reader.readLine();
		}
		Assert.assertTrue( found, "Expected to find rené" );

		patchFile.close();
	}

	@Test
	public void testUtf16NoBom() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "patch-utf-16-nobom-1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.read();
		Assert.assertNull( patchFile.file.getBOM() );
		Assert.assertEquals( patchFile.file.getEncoding(), "UTF-16LE" );

		RandomAccessLineReader reader = patchFile.file;
		reader.gotoLine( 1 );
		boolean found = false;
		String line = reader.readLine();
		while( line != null )
		{
			if( line.contains( "rené" ) )
				found = true;
			line = reader.readLine();
		}
		Assert.assertTrue( found, "Expected to find rené" );

		patchFile.close();
	}
}
