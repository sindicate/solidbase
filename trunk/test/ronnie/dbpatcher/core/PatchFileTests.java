package ronnie.dbpatcher.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PatchFileTests
{
	@Test
	public void testCollectTipVersions()
	{
		PatchFile patchFile = new PatchFile( null );
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

		Set< String > tips = new HashSet();
		patchFile.collectTargets( "1.1", null, true, tips );
		for( String tip : tips )
			System.out.println( tip );

		Set< String > expected = new HashSet();
		expected.add( "1.5" );
		expected.add( "2.5" );
		expected.add( "3.2" );

		Assert.assertEquals( tips, expected );
	}
}
