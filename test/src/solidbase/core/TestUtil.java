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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.testng.Assert;

import solidbase.core.Patcher;
import solidbase.core.SystemException;


public class TestUtil
{
	static public void shutdownHSQLDB( Patcher patcher ) throws SQLException
	{
		Connection connection = patcher.currentDatabase.getConnection( "sa" );
		try
		{
			connection.createStatement().executeUpdate( "SHUTDOWN" );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
		finally
		{
			connection.close();
		}
	}

	static public Connection getConnection( Database database )
	{
		return database.getConnection();
	}

	static public void assertRecordCount( Database database, String tableName, int expected ) throws SQLException
	{
		String sql = "SELECT COUNT(*) FROM " + tableName;
		Connection connection = database.getConnection( database.defaultUser );
		PreparedStatement statement = connection.prepareStatement( sql );
		ResultSet result = statement.executeQuery();
		Assert.assertTrue( result.next() );
		int count = result.getInt( 1 );
		Assert.assertEquals( count, expected );
	}

	static public void verifyVersion( Patcher patcher, String version, String target, int statements, String spec ) throws SQLException
	{
		String sql = "SELECT * FROM DBVERSION";
		Connection connection = patcher.currentDatabase.getConnection();
		PreparedStatement statement = connection.prepareStatement( sql );
		ResultSet result = statement.executeQuery();
		Assert.assertTrue( result.next() );
		Assert.assertEquals( result.getString( "VERSION" ), version, "version:" );
		Assert.assertEquals( result.getString( "TARGET" ), target, "target:" );
		Assert.assertEquals( result.getInt( "STATEMENTS" ), statements, "statements:" );
		if( spec == null )
			Assert.assertFalse( Util.hasColumn( result, "SPEC" ), "SPEC column should not exist in the DBVERSION table" );
		else
			Assert.assertEquals( result.getString( "SPEC" ), spec, "spec:" );
		Assert.assertFalse( result.next() );
	}

	public static void verifyHistoryIncludes( Patcher patcher, String version )
	{
		Assert.assertTrue( patcher.dbVersion.logContains( version ), "Expecting version " + version + " to be part of the history" );
	}

	public static void verifyHistoryNotIncludes( Patcher patcher, String version )
	{
		Assert.assertFalse( patcher.dbVersion.logContains( version ), "Not expecting version " + version + " to be part of the history" );
	}

	static public void assertPatchFileClosed( Patcher patcher )
	{
		Assert.assertNull( patcher.patchFile.file );
	}
}
