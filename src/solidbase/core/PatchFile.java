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

package solidbase.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;

import solidbase.core.Patch.Type;


/**
 * This class manages the patch file contents and the paths between versions.
 * 
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile
{
	static private final Pattern encodingPattern = Pattern.compile( "^--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\"[ \t]*$", Pattern.CASE_INSENSITIVE );

	static private final Pattern patchDefinitionMarkerPattern = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static private final Pattern patchDefinitionPattern = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) [OPEN] \"...\" --> \"...\"";

	static private final Pattern patchStartMarkerPattern = Pattern.compile( "--\\*[ \t]*(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static private final Pattern patchStartPattern = Pattern.compile( "--\\*[ \t]*(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) \"...\" --> \"...\"";

	static private final Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

	static private final Pattern goPattern = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );

	/**
	 * All normal patches in a map indexed by source version.
	 */
	protected MultiValueMap patches = new MultiValueMap();

	/**
	 * All init patches in a map indexed by source version.
	 */
	protected MultiValueMap inits = new MultiValueMap();

	/**
	 * The underlying file.
	 */
	protected RandomAccessLineReader file;


	/**
	 * Creates an new instance of a patch file.
	 * 
	 * @param file The reader which is used to read the contents of the file.
	 */
	protected PatchFile( RandomAccessLineReader file )
	{
		this.file = file;

		try
		{
			String line = file.readLine();
			//System.out.println( "First line [" + line + "]" );
			StringBuilder s = new StringBuilder();
			char[] chars = line.toCharArray();
			for( char c : chars )
				if( c != 0 )
					s.append( c );

			line = s.toString();
			//System.out.println( "First line (fixed) [" + line + "]" );
			Matcher matcher = encodingPattern.matcher( line );
			if( matcher.matches() )
			{
				file.reOpen( matcher.group( 1 ) );
				file.readLine(); // skip the first line
			}
			else
				file.gotoLine( 1 );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}


	/**
	 * Close the patch file. This will also close the underlying file.
	 */
	protected void close()
	{
		if( this.file != null )
		{
			try
			{
				this.file.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			this.file = null;
		}
	}


	/**
	 * Reads and analyzes the patch file. The result is that the patches map is filled with the available patches.
	 */
	protected void read()
	{
		boolean withinDefinition = false;
		boolean definitionComplete = false;
		while( !definitionComplete )
		{
			String line;
			try
			{
				line = this.file.readLine();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			Assert.isTrue( line != null, "End-of-file found before reading a complete definition" );

			if( line.trim().length() > 0 )
			{
				Assert.isTrue( line.startsWith( "--*" ), "Line should start with --*" );
				line = line.substring( 3 ).trim();
				if( line.equalsIgnoreCase( "DEFINITION" ) || line.equalsIgnoreCase( "PATCHES" ) )
				{
					Assert.isFalse( withinDefinition, "Already within the definition" );
					withinDefinition = true;
				}
				else if( line.startsWith( "//" ) )
				{
					// ignore line
				}
				else if( patchDefinitionMarkerPattern.matcher( line ).matches() )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );

					Matcher matcher = patchDefinitionPattern.matcher( line );
					Assert.isTrue( matcher.matches(), PATCH_DEFINITION_SYNTAX_ERROR );
					String action = matcher.group( 1 );
					boolean open = matcher.group( 2 ) != null;
					String source = matcher.group( 3 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 4 );
					Type type = stringToType( action );
					Patch patch = new Patch( type, source, target, open );
					if( type == Type.INIT )
						this.inits.put( source, patch );
					else
						this.patches.put( source, patch );
				}
				else if( line.equalsIgnoreCase( "/DEFINITION" ) || line.equalsIgnoreCase( "/PATCHES" ) )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );
					definitionComplete = true;
				}
				else
					throw new SystemException( "Unexpected line within definition: " + line );
			}
		}

		scan();

		Iterator< Patch > iterator = this.patches.values().iterator();
		while( iterator.hasNext() )
		{
			Patch patch = iterator.next();
			if( patch.getLineNumber() < 0 )
				throw new FatalException( "Upgrade block \"" + StringUtils.defaultString( patch.getSource() ) + "\" --> \"" + patch.getTarget() + "\" not found" );
		}

		iterator = this.inits.values().iterator();
		while( iterator.hasNext() )
		{
			Patch patch = iterator.next();
			if( patch.getLineNumber() < 0 )
				throw new FatalException( "Init block \"" + StringUtils.defaultString( patch.getSource() ) + "\" --> \"" + patch.getTarget() + "\" not found" );
		}
	}


	/**
	 * Translates a patch type string to a type enum.
	 * 
	 * @param type A patch type string.
	 * @return The corresponding type enum.
	 */
	protected Type stringToType( String type )
	{
		if( "UPGRADE".equalsIgnoreCase( type ) || "PATCH".equalsIgnoreCase( type ) )
			return Type.UPGRADE;
		if( "SWITCH".equalsIgnoreCase( type ) || "BRANCH".equalsIgnoreCase( type ) || "RETURN".equalsIgnoreCase( type ) )
			return Type.SWITCH;
		if( "DOWNGRADE".equalsIgnoreCase( type ) )
			return Type.DOWNGRADE;
		if( "INIT".equalsIgnoreCase( type ) )
			return Type.INIT;
		Assert.fail( "Unexpected block type '" + type + "'" );
		return null;
	}


	/**
	 * Scans for patches in the file. Called by {@link #read()}.
	 */
	protected void scan()
	{
		try
		{
			String line = this.file.readLine();
			while( line != null )
			{
				if( line.startsWith( "--*" ) )
				{
					if( patchStartMarkerPattern.matcher( line ).matches() )
					{
						int pos = this.file.getLineNumber() - 1;
						Matcher matcher = patchStartPattern.matcher( line );
						Assert.isTrue( matcher.matches(), PATCH_START_SYNTAX_ERROR, pos );
						String action = matcher.group( 1 );
						String source = matcher.group( 2 );
						String target = matcher.group( 3 );
						Type type = stringToType( action );
						Patch patch;
						if( type == Type.INIT )
						{
							patch = getInitPatch( source.length() == 0 ? null : source, target );
							if( patch == null )
								throw new FatalException( "Undefined init block found: \"" + source + "\" --> \"" + target + "\" at line " + pos );
						}
						else
						{
							patch = getPatch( source.length() == 0 ? null : source, target );
							if( patch == null )
								throw new FatalException( "Undefined upgrade block found: \"" + source + "\" --> \"" + target + "\" at line " + pos );
							if( patch.getType() != type )
								throw new FatalException( "Upgrade block type '" + action + "' is different from its definition at line " + pos );
						}
						if( patch.getLineNumber() >= 0 )
							throw new FatalException( "Double upgrade block \"" + source + "\" --> \"" + target + "\" found at line " + pos );
						patch.setLineNumber( pos );
					}
				}

				line = this.file.readLine();
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}


	/**
	 * Returns the patch belonging to the specified source and target. Also checks for duplicates.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @return The corresponding patch.
	 */
	protected Patch getPatch( String source, String target )
	{
		Patch result = null;

		List patches = (List)this.patches.get( source );
		if( patches != null )
			for( Iterator iter = patches.iterator(); iter.hasNext(); )
			{
				Patch patch = (Patch)iter.next();
				if( patch.getTarget().equals( target ) )
				{
					Assert.isTrue( result == null, "Duplicate upgrade block found", patch.getLineNumber() );
					result = patch;
				}
			}

		return result;
	}


	/**
	 * Returns the patch belonging to the specified source and target. Also checks for duplicates.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @return The corresponding patch.
	 */
	protected Patch getInitPatch( String source, String target )
	{
		Patch result = null;

		List patches = (List)this.inits.get( source );
		if( patches != null )
			for( Iterator iter = patches.iterator(); iter.hasNext(); )
			{
				Patch patch = (Patch)iter.next();
				if( patch.getTarget().equals( target ) )
				{
					Assert.isTrue( result == null, "Duplicate init block found", patch.getLineNumber() );
					result = patch;
				}
			}

		return result;
	}


	protected class GetPatchPath2Result
	{
		protected boolean hasDowngrade;
		protected int switches;
		protected List< Patch > path;

		protected GetPatchPath2Result()
		{
			this.path = new ArrayList< Patch >();
		}

		protected GetPatchPath2Result( Patch patch, GetPatchPath2Result result )
		{
			this.path = new ArrayList< Patch >();
			this.path.add( patch );
			this.path.addAll( result.path );

			for( Patch p : this.path )
				calc( p );
		}

		protected void calc( Patch patch )
		{
			if( patch.isDowngrade() )
				this.hasDowngrade = true;
			else if( patch.isSwitch() )
				this.switches++;
		}

		protected void add( Patch patch )
		{
			this.path.add( patch );
			calc( patch );
		}

		protected void add( GetPatchPath2Result other )
		{
			this.path.addAll( other.path );

			for( Patch p : other.path )
				calc( p );
		}

		protected boolean betterThan( GetPatchPath2Result other )
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
	}


	/**
	 * Returns a patch path for the specified source and target.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @param downgradesAllowed Allow downgrades in the resulting path.
	 * @return A list of patches that correspond to the given source and target versions.
	 */
	protected List< Patch > getPatchPath( String source, String target, boolean downgradesAllowed )
	{
		Set< String > done = new HashSet();
		done.add( source );
		GetPatchPath2Result result = getPatchPath0( source, target, downgradesAllowed, done );
		if( result == null )
			return null;
		return result.path;
	}


	protected GetPatchPath2Result getPatchPath0( String source, String target, boolean downgradesAllowed, Set< String > done )
	{
		GetPatchPath2Result result = new GetPatchPath2Result();

		// If equal than we are finished
		if( source == null )
		{
			if( target == null )
				return result;
		}
		else
		{
			if( source.equals( target ) )
				return result;
		}

		// Start with all the patches that start with the given source
		List< Patch > patches = (List)this.patches.get( source );
		while( patches != null && patches.size() == 1 )
		{
			Patch patch = patches.get( 0 );
			if( done.contains( patch.getTarget() ) )
				return null;
			done.add( patch.getTarget() );
			result.add( patch );
			if( target.equals( patch.getTarget() ) )
				return result; // Done
			patches = (List)this.patches.get( patch.getTarget() );
		}

		// If no patch then return null
		if( patches == null )
			return null;

		GetPatchPath2Result selected = null;

		for( Patch patch : patches )
		{
			if( done.contains( patch.getTarget() ) )
				continue;
			Set< String > done2 = new HashSet();
			done2.addAll( done );
			done2.add( patch.getTarget() );
			GetPatchPath2Result r = getPatchPath0( patch.getTarget(), target, downgradesAllowed, done2 );
			if( r != null )
			{
				r = new GetPatchPath2Result( patch, r );
				if( selected == null )
					selected = r;
				else if( r.betterThan( selected ) )
					selected = r;
			}
		}

		if( selected == null )
			return null;

		result.add( selected );
		return result;
	}


	/**
	 * Returns an init patch path for the specified source. As initialization is always done to the latest version, no target is needed.
	 * 
	 * @param source The source version.
	 * @return A list of init patches that correspond to the given source.
	 */
	protected List getInitPath( String source )
	{
		List< Patch > result = new ArrayList< Patch >();

		// Not expecting branches

		// Start with all the patches that start with the given source
		List patches = (List)this.inits.get( source );
		while( patches != null )
		{
			Assert.isTrue( patches.size() == 1, "Expecting exactly 1" );
			Patch patch = (Patch)patches.get( 0 );
			result.add( patch );
			patches = (List)this.inits.get( patch.getTarget() );
		}

		if( result.size() > 0 )
			return result;
		return null;
	}


	/**
	 * Determines all possible target versions from the specified source version. The current version is also considered.
	 * 
	 * @param version Current version.
	 * @param targeting Indicates that we are already targeting a specific version.
	 * @param tips Only return tip versions.
	 * @param downgradesAllowed Allow downgrades.
	 * @param prefix Only consider versions that start with the given prefix.
	 * @param result All results are added to this set.
	 */
	protected void collectTargets( String version, String targeting, boolean tips, boolean downgradesAllowed, String prefix, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		collectReachableVersions( version, targeting, downgradesAllowed, result );

		if( prefix != null )
			for( Iterator iterator = result.iterator(); iterator.hasNext(); )
				if( !((String)iterator.next()).startsWith( prefix ) )
					iterator.remove();

		if( tips )
			for( Iterator iterator = result.iterator(); iterator.hasNext(); )
			{
				String v = (String)iterator.next();
				List< Patch > patches = (List)this.patches.get( v );
				if( patches != null )
					for( Patch patch : patches )
						if( patch.isUpgrade() )
							if( prefix == null || patch.getTarget().startsWith( prefix ) )
							{
								iterator.remove();
								break;
							}
			}
	}


	/**
	 * Gets all versions that are reachable from the given source version.
	 * 
	 * @param source The source version.
	 * @param targeting Indicates that we are already targeting a specific version.
	 * @param downgradesAllowed Allow downgrades.
	 * @return Returns an ordered set of versions that are reachable from the given source version.
	 */
	protected LinkedHashSet< String > getReachableVersions( String source, String targeting, boolean downgradesAllowed )
	{
		LinkedHashSet result = new LinkedHashSet();
		collectReachableVersions( source, targeting, downgradesAllowed, result );
		return result;
	}


	/**
	 * Retrieves all versions that are reachable from the given source version.
	 * 
	 * @param source The source version.
	 * @param targeting Already targeting a specific version.
	 * @param downgradesAllowed Allow downgrades.
	 * @param result This set gets filled with all versions that are reachable from the given source version.
	 */
	protected void collectReachableVersions( String source, String targeting, boolean downgradesAllowed, Set< String > result )
	{
		if( targeting == null && source != null )
			result.add( source );

		List< Patch > patches = (List)this.patches.get( source ); // Get all patches with the given source
		if( patches == null )
			return;

		LinkedList< Patch > queue = new LinkedList();
		if( targeting != null )
		{
			for( Patch patch : patches )
				if( targeting.equals( patch.getTarget() ) )
					queue.push( patch );
			Assert.isFalse( queue.isEmpty() );
		}
		else
			queue.addAll( patches );

		while( !queue.isEmpty() )
		{
			Patch patch = queue.pop();
			if( !result.contains( patch.getTarget() ) ) // Already there?
				if( downgradesAllowed || !patch.isDowngrade() ) // Downgrades?
				{
					result.add( patch.getTarget() );
					if( !patch.isOpen() ) // Stop when patch is open.
					{
						patches = (List)this.patches.get( patch.getTarget() );
						if( patches != null )
						{
							int size = patches.size();
							while( size > 0 )
								queue.push( patches.get( --size ) );
						}
					}
				}
		}
	}

	/**
	 * Jump to the position in the patch file where the given patch starts.
	 * 
	 * @param patch The patch to jump to.
	 */
	protected void gotoPatch( Patch patch )
	{
		Assert.isTrue( patch.getLineNumber() >= 0, "Upgrade or init block not found" );

		try
		{
			this.file.gotoLine( patch.getLineNumber() );
			String line = this.file.readLine();
			//			System.out.println( line );
			Assert.isTrue( patchStartMarkerPattern.matcher( line ).matches() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	/**
	 * Reads a statement from the patch file.
	 * 
	 * @return A statement from the patch file or null when no more statements are available.
	 */
	protected Command readStatement()
	{
		StringBuilder result = new StringBuilder();
		int pos = 0; // No line found yet

		while( true )
		{
			try
			{
				String line = this.file.readLine();
				Assert.notNull( line, "Premature end of file found" );

				if( line.trim().length() == 0 )
					continue;

				if( goPattern.matcher( line ).matches() )
				{
					if( pos == 0 )
						pos = this.file.getLineNumber() - 1;
					return new Command( result.toString(), false, pos );
				}

				if( patchEndPattern.matcher( line ).matches() )
				{
					if( result.length() > 0 )
						throw new UnterminatedStatementException( this.file.getLineNumber() - 1 );
					return null;
				}

				if( line.startsWith( "--*" ) )
				{
					if( result.length() > 0 )
						throw new UnterminatedStatementException( this.file.getLineNumber() - 1 );

					if( !patchStartPattern.matcher( line ).matches() ) // skip patch start
					{
						line = line.substring( 3 ).trim();
						if( !line.startsWith( "//" )) // skip comment
						{
							if( pos == 0 )
								pos = this.file.getLineNumber() - 1;
							return new Command( line, true, pos );
						}
					}
					continue;
				}

				if( pos == 0 )
					pos = this.file.getLineNumber() - 1;
				result.append( line );
				result.append( '\n' );
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
	}


	/**
	 * Gets the encoding of the patch file.
	 * 
	 * @return The encoding of the patch file.
	 */
	public String getEncoding()
	{
		return this.file.getEncoding();
	}
}