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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.Patch.Type;
import solidbase.util.RandomAccessLineReader;


/**
 * Tests {@link PatchFile}.
 * 
 * @author René M. de Bloois
 */
public class PatchFileTests
{
	/**
	 * Tests whether the {@link PatchFile} returns the correct set of tip targets.
	 * 
	 * @throws IOException Whenever it needs to.
	 */
	@Test
	public void testCollectTipVersions1() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "testpatch1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.scan();
		patchFile.close();

		Map< String, Collection< Patch > > patches = patchFile.patches;
		put( patches, "1.1", new Patch( Type.UPGRADE, "1.1", "1.2", false ) );
		put( patches, "1.2", new Patch( Type.UPGRADE, "1.2", "1.3", false ) );
		put( patches, "1.3", new Patch( Type.UPGRADE, "1.3", "1.4", false ) ); // branch
		put( patches, "1.4", new Patch( Type.UPGRADE, "1.4", "1.5", false ) );
		put( patches, "1.5", new Patch( Type.SWITCH, "1.5", "2.1", false ) );
		put( patches, "1.3", new Patch( Type.UPGRADE, "1.3", "2.1", false ) );
		put( patches, "2.1", new Patch( Type.UPGRADE, "2.1", "2.2", false ) );
		put( patches, "2.2", new Patch( Type.UPGRADE, "2.2", "2.3", false ) );
		put( patches, "2.3", new Patch( Type.UPGRADE, "2.3", "2.4", false ) ); // branch
		put( patches, "2.4", new Patch( Type.UPGRADE, "2.4", "2.5", false ) );
		put( patches, "2.5", new Patch( Type.SWITCH, "2.5", "3.1", false ) );
		put( patches, "2.3", new Patch( Type.UPGRADE, "2.3", "3.1", false ) );
		put( patches, "3.1", new Patch( Type.UPGRADE, "3.1", "3.2", false ) );

		patchFile.versions.add( "1.1" );
		patchFile.versions.add( "1.3" );

		Set< String > result = new HashSet< String >();
		patchFile.collectTargets( "1.1", null, true, false, null, result );
		for( String tip : result )
			System.out.println( tip );

		Set< String > expected = new HashSet< String >();
		expected.add( "1.5" );
		expected.add( "2.5" );
		expected.add( "3.2" );

		Assert.assertEquals( result, expected );

		// Another one

		result = new HashSet< String >();
		patchFile.collectTargets( "1.3", "2.1", true, false, null, result );
		for( String tip : result )
			System.out.println( tip );

		expected = new HashSet< String >();
		expected.add( "2.5" );
		expected.add( "3.2" );

		Assert.assertEquals( result, expected );

		// Check the path

		Path path = patchFile.getPatchPath( "1.3", "2.1", false );
		Assert.assertEquals( path.size(), 1 );
		Assert.assertEquals( path.iterator().next().getTarget(), "2.1" );
	}

	static public void put( Map< String, Collection< Patch > > map, String key, Patch value )
	{
		Collection< Patch > patches = map.get( key );
		if( patches == null )
			map.put( key, patches = new LinkedList< Patch >() );
		patches.add( value );
	}

	/**
	 * Tests whether the {@link PatchFile} returns the correct set of tip targets. This one specifies a target wildcard.
	 * 
	 * @throws IOException Whenever it needs to.
	 */
	@Test
	public void testCollectTipVersions2() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "testpatch1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.scan();
		patchFile.close();

		Map< String, Collection< Patch > > patches = patchFile.patches = new HashMap< String, Collection< Patch > >();
		put( patches, "1.1", new Patch( Type.UPGRADE, "1.1", "1.2", false ) );
		put( patches, "1.2", new Patch( Type.UPGRADE, "1.2", "1.3", false ) );
		put( patches, "1.3", new Patch( Type.UPGRADE, "1.3", "1.4", false ) );
		put( patches, "1.4", new Patch( Type.UPGRADE, "1.4", "2.1", false ) );
		put( patches, "2.1", new Patch( Type.UPGRADE, "2.1", "2.2", false ) );
		put( patches, "2.2", new Patch( Type.UPGRADE, "2.2", "2.3", false ) );
		put( patches, "2.3", new Patch( Type.UPGRADE, "2.3", "2.4", false ) );

		patchFile.versions.add( "1.1" );

		Set< String > result = new HashSet< String >();
		patchFile.collectTargets( "1.1", null, true, false, "1.", result );
		for( String tip : result )
			System.out.println( tip );

		Set< String > expected = new HashSet< String >();
		expected.add( "1.4" );

		Assert.assertEquals( result, expected );
	}

	/**
	 * Tests whether {@link PatchFile} returns the correct set of targets. This one has an open patch.
	 * 
	 * @throws IOException Whenever it needs to.
	 */
	@Test
	public void testOpenPatch() throws IOException
	{
		RandomAccessLineReader ralr = new RandomAccessLineReader( new File( "testpatch1.sql" ) );
		PatchFile patchFile = new PatchFile( ralr );
		patchFile.close();

		Map< String, Collection< Patch > > patches = patchFile.patches;
		put( patches, "1.1", new Patch( Type.UPGRADE, "1.1", "1.2", false ) );
		put( patches, "1.2", new Patch( Type.UPGRADE, "1.2", "1.3", false ) );
		put( patches, "1.3", new Patch( Type.UPGRADE, "1.3", "1.4", false ) ); // branch
		put( patches, "1.4", new Patch( Type.UPGRADE, "1.4", "1.5", false ) );
		put( patches, "1.5", new Patch( Type.SWITCH, "1.5", "2.1", false ) );
		put( patches, "1.3", new Patch( Type.UPGRADE, "1.3", "2.1", false ) );
		put( patches, "2.1", new Patch( Type.UPGRADE, "2.1", "2.2", true ) ); // open
		put( patches, "2.2", new Patch( Type.UPGRADE, "2.2", "2.3", false ) );
		put( patches, "2.3", new Patch( Type.UPGRADE, "2.3", "2.4", false ) ); // branch
		put( patches, "2.4", new Patch( Type.UPGRADE, "2.4", "2.5", false ) );
		put( patches, "2.5", new Patch( Type.SWITCH, "2.5", "3.1", false ) );
		put( patches, "2.3", new Patch( Type.UPGRADE, "2.3", "3.1", false ) );
		put( patches, "3.1", new Patch( Type.UPGRADE, "3.1", "3.2", false ) );

		patchFile.versions.addAll( patches.keySet() );
		patchFile.versions.add( "3.2" );

		Set< String > result = new HashSet< String >();
		patchFile.collectTargets( "1.1", null, false, false, null, result );
		for( String target : result )
			System.out.println( target );

		Set< String > expected = new HashSet< String >();
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
