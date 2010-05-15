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
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import solidbase.core.Patch.Type;


/**
 * This class manages the patch file contents and the paths between versions.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile extends SQLFile
{
	static private final Pattern PATCH_DEFINITION_MARKER_PATTERN = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static private final Pattern PATCH_DEFINITION_PATTERN = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) [OPEN] \"...\" --> \"...\"";

	static private final Pattern CONTROL_TABLES_PATTERN = Pattern.compile( "VERSION\\s+TABLE\\s+(\\S+)\\s+LOG\\s+TABLE\\s+(\\S+)", Pattern.CASE_INSENSITIVE );

	static private final Pattern PATCH_START_MARKER_PATTERN = Pattern.compile( "--\\*[ \t]*(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static private final Pattern PATCH_START_PATTERN = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) \"...\" --> \"...\"";

	static private final Pattern PATCH_END_PATTERN = Pattern.compile( "/(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

	/**
	 * All normal patches in a map indexed by source version.
	 */
	protected MultiValueMap patches = new MultiValueMap();

	/**
	 * Contains all known versions from the upgrade file.
	 */
	protected Set< String > versions = new HashSet< String >();

	/**
	 * All init patches in a map indexed by source version.
	 */
	protected MultiValueMap inits = new MultiValueMap();

	/**
	 * The name of the version control table as defined in the upgrade file.
	 */
	protected String versionTableName;

	/**
	 * The name of the log control table as defined in the upgrade file.
	 */
	protected String logTableName;


	/**
	 * Creates an new instance of a patch file.
	 * 
	 * @param file The reader which is used to read the contents of the file.
	 */
	protected PatchFile( RandomAccessLineReader file )
	{
		super( file );
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
			if( line == null )
				throw new CommandFileException( "Unexpected EOF found", this.file.getLineNumber() );

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
				else if( PATCH_DEFINITION_MARKER_PATTERN.matcher( line ).matches() )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );

					Matcher matcher = PATCH_DEFINITION_PATTERN.matcher( line );
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
					{
						this.patches.put( source, patch );
						this.versions.add( source );
						this.versions.add( target );
					}
				}
				else if( line.equalsIgnoreCase( "/DEFINITION" ) || line.equalsIgnoreCase( "/PATCHES" ) )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );
					definitionComplete = true;
				}
				else if( withinDefinition )
				{
					Matcher matcher = CONTROL_TABLES_PATTERN.matcher( line );
					if( matcher.matches() )
					{
						this.versionTableName = matcher.group( 1 );
						this.logTableName = matcher.group( 2 );
					}
					else
						throw new SystemException( "Unexpected line within definition: " + line );
				}
				else
					throw new SystemException( "Unexpected line outside definition: " + line );
			}
		}

		scan();

		// Check that all defined upgrade blocks are found
		Iterator< Patch > iterator = this.patches.values().iterator();
		while( iterator.hasNext() )
		{
			Patch patch = iterator.next();
			if( patch.getLineNumber() < 0 )
				throw new FatalException( "Upgrade block \"" + StringUtils.defaultString( patch.getSource() ) + "\" --> \"" + patch.getTarget() + "\" not found" );
		}

		// Check that all defined init blocks are found
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
					if( PATCH_START_MARKER_PATTERN.matcher( line ).matches() )
					{
						line = line.substring( 3 ).trim();
						int pos = this.file.getLineNumber() - 1;
						Matcher matcher = PATCH_START_PATTERN.matcher( line );
						if( !matcher.matches() )
							throw new CommandFileException( PATCH_START_SYNTAX_ERROR, pos );
						String action = matcher.group( 1 );
						String source = matcher.group( 2 );
						String target = matcher.group( 3 );
						Type type = stringToType( action );
						Patch patch;
						if( type == Type.INIT )
						{
							patch = getInitPatch( source.length() == 0 ? null : source, target );
							if( patch == null )
								throw new CommandFileException( "Undefined init block found: \"" + source + "\" --> \"" + target + "\"", pos );
						}
						else
						{
							patch = getPatch( source.length() == 0 ? null : source, target );
							if( patch == null )
								throw new CommandFileException( "Undefined upgrade block found: \"" + source + "\" --> \"" + target + "\"", pos );
							if( patch.getType() != type )
								throw new CommandFileException( "Upgrade block type '" + action + "' is different from its definition", pos );
						}
						if( patch.getLineNumber() >= 0 )
							throw new CommandFileException( "Duplicate upgrade block \"" + source + "\" --> \"" + target + "\" found", pos );
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


	/**
	 * Determine the best path between a source version and a target version.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @param downgradesAllowed Allow downgrades in the resulting path.
	 * @return The best path between a source version and a target version. This path can be empty when the source and
	 *         target are equal. The result will be null if there is no path.
	 */
	protected Path getPatchPath( String source, String target, boolean downgradesAllowed )
	{
		Set< String > done = new HashSet();
		done.add( source );
		return getPatchPath0( source, target, downgradesAllowed, done );
	}


	/**
	 * Determine the best path between a source version and a target version.
	 * 
	 * @param source The source version.
	 * @param target The target version.
	 * @param downgradesAllowed Allow downgrades in the resulting path.
	 * @param targetsProcessed A set of targets that have already been processed. This prevents endless loops.
	 * @return The best path between a source version and a target version. This path can be empty when the source and
	 *         target are equal. The result will be null if there is no path.
	 */
	protected Path getPatchPath0( String source, String target, boolean downgradesAllowed, Set< String > targetsProcessed )
	{
		Path result = new Path();

		// If equal than return an empty path
		if( ObjectUtils.equals( source, target ) )
			return result;

		// Start with all the patches that have the given source
		List< Patch > patches = (List)this.patches.get( source );

		// As long as only one patch found loop instead of recursion
		while( patches != null && patches.size() == 1 )
		{
			Patch patch = patches.get( 0 );

			if( targetsProcessed.contains( patch.getTarget() ) ) // Target already processed -> no path found
				return null;

			targetsProcessed.add( patch.getTarget() ); // Register target

			result.append( patch ); // Append to result
			if( target.equals( patch.getTarget() ) ) // Target is requested target -> return result
				return result;

			patches = (List)this.patches.get( patch.getTarget() );
		}

		// No patches -> no path found
		if( patches == null )
			return null;

		// More then one patch found, select the best one
		Path selected = null;
		for( Patch patch : patches )
		{
			if( targetsProcessed.contains( patch.getTarget() ) ) // Target already processed -> ignore
				continue;

			// Build new set for recursive call
			Set< String > processed = new HashSet();
			processed.addAll( targetsProcessed );
			processed.add( patch.getTarget() );

			// Call recursive and select if better
			Path path = getPatchPath0( patch.getTarget(), target, downgradesAllowed, processed );
			if( path != null )
			{
				path.prepend( patch );
				if( selected == null )
					selected = path;
				else if( path.betterThan( selected ) )
					selected = path;
			}
		}

		// No patches found -> no path found
		if( selected == null )
			return null;

		// Return the selected path appended to the first path
		return result.append( selected );
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

		if( result.size() == 0 )
			throw new FatalException( "The current database version (" + StringUtils.defaultString( version, "no version" ) + ") is not available in the upgrade file. Maybe this version is deprecated or the wrong upgrade file is used." );

		if( prefix != null )
			for( Iterator< String > iterator = result.iterator(); iterator.hasNext(); )
			{
				String s = iterator.next();
				if( s == null || !s.startsWith( prefix ) )
					iterator.remove();
			}

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
		if( targeting == null && this.versions.contains( source ) )
			result.add( source ); // The source is recognized.

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
			Assert.isTrue( PATCH_START_MARKER_PATTERN.matcher( line ).matches() );
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
	@Override
	protected Command readStatement()
	{
		Command command;

		do
		{
			command = super.readStatement();
			Assert.notNull( command, "Premature end of file found" );

			if( command.isTransient() )
			{
				if( PATCH_END_PATTERN.matcher( command.getCommand() ).matches() )
					return null;
				if( PATCH_START_PATTERN.matcher( command.getCommand() ).matches() )
					command = null;
			}
		}
		while( command == null );

		return command;
	}
}
