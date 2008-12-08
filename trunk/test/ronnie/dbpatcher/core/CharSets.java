package ronnie.dbpatcher.core;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.logicacmg.idt.commons.io.RandomAccessLineReader;

import ronnie.dbpatcher.test.core.TestProgressListener;

public class CharSets
{
	@Test
	public void testIso8859() throws IOException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.openPatchFile( "testpatch1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "ISO-8859-1" );
	}

	@Test
	public void testUtf8() throws IOException, SQLException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.setConnection( new Database( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testUtf8" ), "sa", null );

		Patcher.openPatchFile( "patch-utf-8-1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-8" );
		try
		{
			Set< String > targets = Patcher.getTargets( false );
			assert targets.size() > 0;

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
	public void testUtf16Bom() throws IOException, SQLException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
		Patcher.openPatchFile( "patch-utf-16-bom-1.sql" );
		Assert.assertEquals( Patcher.patchFile.file.getEncoding(), "UTF-16LE" );
	}

	@Test
	public void testUtf16BomAndExplicit() throws IOException, SQLException
	{
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
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
		Patcher.end();
		Patcher.setCallBack( new TestProgressListener() );
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
