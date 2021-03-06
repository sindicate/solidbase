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
import org.testng.annotations.Test;

import solidstack.io.FileResource;
import solidstack.io.RandomAccessSourceReader;


public class CharSets
{
	@Test
	public void testIso8859() throws IOException
	{
		RandomAccessSourceReader ralr = new RandomAccessSourceReader( new FileResource( "testpatch1.sql" ), EncodingDetector.INSTANCE );
		UpgradeFile upgradeFile = new UpgradeFile( ralr );
		upgradeFile.scan();
		Assert.assertEquals( upgradeFile.reader.getEncoding(), "ISO-8859-1" );
		upgradeFile.close();
	}

	@Test
	public void testUtf8() throws SQLException, ProcessException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );

		TestProgressListener progress = new TestProgressListener();
		UpgradeProcessor patcher = Setup.setupUpgradeProcessor( "patch-utf-8-1.sql" );
		UpgradeFile upgradeFile = Factory.openUpgradeFile( new FileResource( "patch-utf-8-1.sql" ), progress );
		patcher.setUpgradeFile( upgradeFile );
		patcher.init();

		Assert.assertEquals( patcher.upgradeFile.reader.getEncoding(), "UTF-8" );
		patcher.upgrade( "1.0.2" );
		Connection connection = patcher.getCurrentDatabase().getConnection();
		Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM USERS" );
		assert result.next();
		String userName = result.getString( "USER_USERNAME" );
		Assert.assertEquals( userName, "rené" );

		patcher.end();
	}

	// TODO Add these tests to solidstack

	@Test
	public void testUtf16Bom() throws IOException
	{
		RandomAccessSourceReader ralr = new RandomAccessSourceReader( new FileResource( "patch-utf-16-bom-1.sql" ), EncodingDetector.INSTANCE );
		UpgradeFile upgradeFile = new UpgradeFile( ralr );
		upgradeFile.scan();
//		Assert.assertEquals( upgradeFile.file.getBOM(), new byte[] { -1, -2 } );
		Assert.assertEquals( upgradeFile.reader.getEncoding(), "UTF-16" );
		upgradeFile.close();
	}

	@Test
	public void testUtf16BomAndExplicit() throws IOException
	{
		RandomAccessSourceReader ralr = new RandomAccessSourceReader( new FileResource( "patch-utf-16-bom-2.sql" ), EncodingDetector.INSTANCE );
		UpgradeFile upgradeFile = new UpgradeFile( ralr );
		upgradeFile.scan();
//		Assert.assertEquals( upgradeFile.file.getBOM(), new byte[] { -1, -2 } );
		Assert.assertEquals( upgradeFile.reader.getEncoding(), "UTF-16LE" );

		RandomAccessSourceReader reader = upgradeFile.reader;
		reader.gotoLine( 1 );
		boolean found = false;
		String line = reader.readLine();
		while( line != null )
		{
			if( line.contains( "René" ) ) {
				found = true;
			}
			line = reader.readLine();
		}
		Assert.assertTrue( found, "Expected to find René" );

		upgradeFile.close();
	}

	@Test
	public void testUtf16NoBom() throws IOException
	{
		RandomAccessSourceReader ralr = new RandomAccessSourceReader( new FileResource( "patch-utf-16-nobom-1.sql" ), EncodingDetector.INSTANCE );
		UpgradeFile upgradeFile = new UpgradeFile( ralr );
		upgradeFile.scan();
//		Assert.assertNull( upgradeFile.file.getBOM() );
		Assert.assertEquals( upgradeFile.reader.getEncoding(), "UTF-16LE" );

		RandomAccessSourceReader reader = upgradeFile.reader;
		reader.gotoLine( 1 );
		boolean found = false;
		String line = reader.readLine();
		while( line != null )
		{
			if( line.contains( "René" ) ) {
				found = true;
			}
			line = reader.readLine();
		}
		Assert.assertTrue( found, "Expected to find René" );

		upgradeFile.close();
	}
}
