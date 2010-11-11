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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import solidbase.util.Assert;


/**
 * An upgrade path.
 * 
 * @author René M. de Bloois
 */
public class Path implements Iterable< Patch >
{
	/**
	 * Does the path contain a downgrade?
	 */
	protected boolean hasDowngrade;

	/**
	 * How many switches does the path contain?
	 */
	protected int switches;

	/**
	 * A list of patches.
	 */
	protected List< Patch > patches;


	/**
	 * Constructor for an empty path.
	 */
	protected Path()
	{
		this.patches = new ArrayList< Patch >();
	}

	/**
	 * Analyzes a patch.
	 * 
	 * @param patch The patch to analyze.
	 */
	protected void analyze( Patch patch )
	{
		if( patch.isDowngrade() )
			this.hasDowngrade = true;
		else if( patch.isSwitch() )
			this.switches++;
	}

	/**
	 * Adds a patch.
	 * 
	 * @param patch The patch to add.
	 * @return The path itself after appending the given patch.
	 */
	protected Path append( Patch patch )
	{
		this.patches.add( patch );
		analyze( patch );
		return this;
	}

	/**
	 * Adds a patch.
	 * 
	 * @param patch The patch to add.
	 * @return The path itself after appending the given patch.
	 */
	protected Path prepend( Patch patch )
	{
		this.patches.add( 0, patch );
		analyze( patch );
		return this;
	}

	/**
	 * Appends a path to the end of this path.
	 * 
	 * @param path The path to append.
	 * @return The path itself after appending the given path.
	 */
	protected Path append( Path path )
	{
		this.patches.addAll( path.patches );
		for( Patch p : path.patches )
			analyze( p );
		return this;
	}

	/**
	 * Is this path better than another?
	 * 
	 * @param other The other path.
	 * @return True if this path is better, false otherwise.
	 */
	protected boolean betterThan( Path other )
	{
		if( other.hasDowngrade )
		{
			if( !this.hasDowngrade )
				return true;
		}
		else
		{
			if( this.hasDowngrade )
				return false;
		}

		if( other.switches > this.switches )
			return true;
		if( other.switches < this.switches )
			return false;
		Assert.fail( "Couldn't decide between 2 upgrade paths" );
		return false;
	}

	/**
	 * Return the number of patches in this path.
	 * 
	 * @return The number of patches in this path.
	 */
	protected int size()
	{
		return this.patches.size();
	}

	/**
	 * Returns an iterator over the patches in this path.
	 * 
	 * @return An iterator over the patches in this path.
	 */
	public Iterator< Patch > iterator()
	{
		return this.patches.iterator();
	}
}
