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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import solidbase.core.Database;
import solidbase.core.Patcher;
import solidbase.core.RandomAccessLineReader;
import solidbase.test.core.TestProgressListener;


public class CharSets
{
	@BeforeMethod
	public void setup()
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
	}

	@Test
	public void testIso8859() throws IOException
	{
		Patcher.openPatchFile( "testpatch1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "ISO-8859-1" );
	}

	@Test
	public void testUtf8() throws IOException, SQLException, SQLExecutionException
	{
		Patcher.setDefaultConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testUtf8", "sa", null ) );

		Patcher.openPatchFile( "patch-utf-8-1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-8" );
		try
		{
			Patcher.patch( "1.0.2" );
		}
		finally
		{
			Patcher.closePatchFile();
		}

		Connection connection = Patcher.database.getConnection( "sa" );
		Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM USERS" );
		assert result.next();
		String userName = result.getString( "USER_USERNAME" );
		Assert.assertEquals( userName, "rené" );
	}

	@Test
	public void testUtf16Bom() throws IOException, SQLExecutionException
	{
		Patcher.openPatchFile( "patch-utf-16-bom-1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-16LE" );
	}

	@Test
	public void testUtf16BomAndExplicit() throws IOException, SQLException
	{
		Patcher.openPatchFile( "patch-utf-16-bom-2.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-16LE" );

		RandomAccessLineReader reader = Patcher.patchFile.file;
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
	}

	@Test
	public void testUtf16NoBom() throws IOException, SQLException
	{
		Patcher.openPatchFile( "patch-utf-16-nobom-1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-16LE" );

		RandomAccessLineReader reader = Patcher.patchFile.file;
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
	}
}
