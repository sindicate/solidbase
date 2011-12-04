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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import mockit.Deencapsulation;

import org.apache.tools.ant.Main;
import org.testng.Assert;


public class TestUtil
{
	static public void shutdownHSQLDB( UpgradeProcessor patcher ) throws SQLException
	{
		Connection connection = patcher.getCurrentDatabase().getConnection();
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
		Connection connection = database.getConnection();
		PreparedStatement statement = connection.prepareStatement( sql );
		ResultSet result = statement.executeQuery();
		Assert.assertTrue( result.next() );
		int count = result.getInt( 1 );
		Assert.assertEquals( count, expected );
	}

	static public void verifyVersion( UpgradeProcessor patcher, String version, String target, int statements, String spec ) throws SQLException
	{
		String sql = "SELECT * FROM DBVERSION";
		Connection connection = patcher.dbVersion.database.getDefaultConnection();
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
		connection.commit();
	}

	public static void verifyHistoryIncludes( UpgradeProcessor patcher, String version )
	{
		Assert.assertTrue( patcher.dbVersion.logContains( version ), "Expecting version " + version + " to be part of the history" );
	}

	public static void verifyHistoryNotIncludes( UpgradeProcessor patcher, String version )
	{
		Assert.assertFalse( patcher.dbVersion.logContains( version ), "Not expecting version " + version + " to be part of the history" );
	}

	static public void assertPatchFileClosed( UpgradeProcessor patcher )
	{
		Assert.assertNull( patcher.upgradeFile.file );
	}

	static public void dropDerbyDatabase( String url ) throws SQLException
	{
		if( !url.contains( "drop=true" ) )
			url = url + ";drop=true";
		try
		{
			DriverManager.getConnection( url, null, null );
		}
		catch( SQLException e )
		{
			if( e.getSQLState().equals( "XJ004" ) ) // "Database 'memory:test' not found."
				return;
			System.out.println( e.getSQLState() );
			throw e;
		}
	}

	static public void dropHSQLDBSchema( String url, String username, String password ) throws SQLException
	{
		try
		{
			Class.forName( "org.hsqldb.jdbcDriver" );
		}
		catch( ClassNotFoundException e )
		{
			throw new SystemException( e );
		}

		try
		{
			Connection connection = DriverManager.getConnection( url, username, password );
			connection.createStatement().execute( "DROP SCHEMA PUBLIC CASCADE" );
		}
		catch( SQLException e )
		{
			System.out.println( e.getSQLState() );
			throw e;
		}
	}

	static public String generalizeOutput( String output )
	{
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "[A-Z]:\\\\\\S+\\\\", "X:/.../" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		output = output.replaceAll( "folder\\\\", "folder/" );
		return output.replaceAll( "\\\r", "" );
	}

	static public void assertQueryResultEquals( UpgradeProcessor patcher, String query, Object expected ) throws SQLException
	{
		Connection connection = patcher.getCurrentDatabase().getConnection();
		ResultSet result = connection.createStatement().executeQuery( query );
		assert result.next() : "Expected 1 row";
		Object value = result.getObject( 1 );
		assert !result.next() : "Expected only 1 row";
		if( expected == null )
			assert value == null : "Expected null, got [" + value + "]";
		else
			assert expected.equals( value ) : "Expected [" + expected + "], got [" + value + "]";
	}

	static public String capture( Runnable runnable )
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		PrintStream print = new PrintStream( buf );
		PrintStream origOut = System.out;
		PrintStream origErr = System.err;
		System.setOut( print );
		System.setErr( print );
		try
		{
			runnable.run();
			print.close();
		}
		finally
		{
			System.setOut( origOut );
			System.setErr( origErr );
		}
		return buf.toString();
	}

	static public String captureAnt( Runnable runnable )
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		PrintStream print = new PrintStream( buf );
		PrintStream origOut = System.out;
		PrintStream origErr = System.err;
		System.setOut( print );
		System.setErr( print );
		Deencapsulation.setField( Main.class, "out", print );
		Deencapsulation.setField( Main.class, "err", print );
		try
		{
			runnable.run();
			print.close();
		}
		finally
		{
			System.setOut( origOut );
			System.setErr( origErr );
		}
		return buf.toString();
	}
}
