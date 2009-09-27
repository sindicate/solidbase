/*--
 * Copyright 2006 Ren� M. de Bloois
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
		expected.add( "1.1" );
		expected.add( "1.2" );
		expected.add( "1.3" );
		expected.add( "1.4" );
		expected.add( "1.5" );
		expected.add( "2.1" );
		expected.add( "2.2" );

		Assert.assertEquals( result, expected );
	}
}
