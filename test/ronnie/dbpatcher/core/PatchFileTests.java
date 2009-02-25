package ronnie.dbpatcher.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PatchFileTests
{
	@Test
	public void testCollectTipVersions1() throws IOException
	{
		Patcher.openPatchFile( "testpatch1.sql" );
		PatchFile patchFile = Patcher.patchFile;

		Map< String, Patch > patches = patchFile.patches;
		patches.put( "1.1", new Patch( "1.1", "1.2", false, false, false, false ) );
		patches.put( "1.2", new Patch( "1.2", "1.3", false, false, false, false ) );
		patches.put( "1.3", new Patch( "1.3", "1.4", true, false, false, false ) );
		patches.put( "1.3", new Patch( "1.3", "2.1", false, false, false, false ) );
		patches.put( "1.4", new Patch( "1.4", "1.5", false, false, false, false ) );
		patches.put( "1.5", new Patch( "1.5", "2.1", false, true, false, false ) );
		patches.put( "2.1", new Patch( "2.1", "2.2", false, false, false, false ) );
		patches.put( "2.2", new Patch( "2.2", "2.3", false, false, false, false ) );
		patches.put( "2.3", new Patch( "2.3", "2.4", true, false, false, false ) );
		patches.put( "2.3", new Patch( "2.3", "3.1", false, false, false, false ) );
		patches.put( "2.4", new Patch( "2.4", "2.5", false, false, false, false ) );
		patches.put( "2.5", new Patch( "2.5", "3.1", false, true, false, false ) );
		patches.put( "3.1", new Patch( "3.1", "3.2", false, false, false, false ) );

		Set< String > result = new HashSet();
		patchFile.collectTargets( "1.1", null, true, null, result );
		for( String tip : result )
			System.out.println( tip );

		Set< String > expected = new HashSet();
		expected.add( "1.5" );
		expected.add( "2.5" );
		expected.add( "3.2" );

		Assert.assertEquals( result, expected );

		// Another one

		result = new HashSet();
		patchFile.collectTargets( "1.3", "2.1", true, null, result );
		for( String tip : result )
			System.out.println( tip );

		expected = new HashSet();
		expected.add( "2.5" );
		expected.add( "3.2" );

		Assert.assertEquals( result, expected );
	}

	@Test
	public void testCollectTipVersions2() throws IOException
	{
		Patcher.openPatchFile( "testpatch1.sql" );
		PatchFile patchFile = Patcher.patchFile;

		Map< String, Patch > patches = patchFile.patches = new MultiValueMap();
		patches.put( "1.1", new Patch( "1.1", "1.2", false, false, false, false ) );
		patches.put( "1.2", new Patch( "1.2", "1.3", false, false, false, false ) );
		patches.put( "1.3", new Patch( "1.3", "1.4", false, false, false, false ) );
		patches.put( "1.4", new Patch( "1.4", "2.1", false, false, false, false ) );
		patches.put( "2.1", new Patch( "2.1", "2.2", false, false, false, false ) );
		patches.put( "2.2", new Patch( "2.2", "2.3", false, false, false, false ) );
		patches.put( "2.3", new Patch( "2.3", "2.4", false, false, false, false ) );

		Set< String > result = new HashSet();
		patchFile.collectTargets( "1.1", null, true, "1.", result );
		for( String tip : result )
			System.out.println( tip );

		Set< String > expected = new HashSet();
		expected.add( "1.4" );

		Assert.assertEquals( result, expected );
	}

	@Test
	public void testOpenPatch() throws IOException
	{
		Patcher.openPatchFile( "testpatch1.sql" );
		PatchFile patchFile = Patcher.patchFile;

		Map< String, Patch > patches = patchFile.patches;
		patches.put( "1.1", new Patch( "1.1", "1.2", false, false, false, false ) );
		patches.put( "1.2", new Patch( "1.2", "1.3", false, false, false, false ) );
		patches.put( "1.3", new Patch( "1.3", "1.4", true, false, false, false ) );
		patches.put( "1.3", new Patch( "1.3", "2.1", false, false, false, false ) );
		patches.put( "1.4", new Patch( "1.4", "1.5", false, false, false, false ) );
		patches.put( "1.5", new Patch( "1.5", "2.1", false, true, false, false ) );
		patches.put( "2.1", new Patch( "2.1", "2.2", false, false, true, false ) );
		patches.put( "2.2", new Patch( "2.2", "2.3", false, false, false, false ) );
		patches.put( "2.3", new Patch( "2.3", "2.4", true, false, false, false ) );
		patches.put( "2.3", new Patch( "2.3", "3.1", false, false, false, false ) );
		patches.put( "2.4", new Patch( "2.4", "2.5", false, false, false, false ) );
		patches.put( "2.5", new Patch( "2.5", "3.1", false, true, false, false ) );
		patches.put( "3.1", new Patch( "3.1", "3.2", false, false, false, false ) );

		Set< String > result = new HashSet();
		patchFile.collectTargets( "1.1", null, false, null, result );
		for( String tip : result )
			System.out.println( tip );

		Set< String > expected = new HashSet();
		expected.add( "1.2" );
		expected.add( "1.3" );
		expected.add( "1.4" );
		expected.add( "1.5" );
		expected.add( "2.1" );
		expected.add( "2.2" );

		Assert.assertEquals( result, expected );
	}
}
