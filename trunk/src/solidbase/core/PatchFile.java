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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;

import solidbase.core.Patch.Type;


/**
 * This class manages the patch file contents and the paths between versions.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile
{
	static protected final Pattern encodingPattern = Pattern.compile( "^--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\"[ \t]*$", Pattern.CASE_INSENSITIVE );

	static protected final Pattern patchDefinitionMarkerPattern = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchDefinitionPattern = Pattern.compile( "(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	private static final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) [OPEN] \"...\" --> \"...\"";

	static protected final Pattern patchStartMarkerPattern = Pattern.compile( "--\\*[ \t]*(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchStartPattern = Pattern.compile( "--\\*[ \t]*(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|UPGRADE|SWITCH|DOWNGRADE) \"...\" --> \"...\"";

	static protected final Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|UPGRADE|SWITCH|DOWNGRADE|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

	static protected final Pattern goPattern = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );

	protected MultiValueMap patches = new MultiValueMap();
	protected MultiValueMap inits = new MultiValueMap();
	protected RandomAccessLineReader file;


	protected PatchFile( RandomAccessLineReader file ) throws IOException
	{
		this.file = file;

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


	protected void close() throws IOException
	{
		if( this.file != null )
		{
			this.file.close();
			this.file = null;
		}
	}


	/**
	 * Reads and analyzes the patch file. The result is that the patches map is filled with the available patches.
	 * 
	 * @throws IOException
	 */
	protected void read() throws IOException
	{
		boolean withinDefinition = false;
		boolean definitionComplete = false;
		while( !definitionComplete )
		{
			String line = this.file.readLine();
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
	}


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
	 * 
	 * @throws IOException
	 */
	protected void scan() throws IOException
	{
		int pos = this.file.getLineNumber();
		String line = this.file.readLine();

		while( line != null )
		{
			if( line.startsWith( "--*" ) )
			{
				if( patchStartMarkerPattern.matcher( line ).matches() )
				{
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
						Assert.isTrue( patch != null, "Undefined init block found: \"" + source + "\" --> \"" + target + "\"" );
					}
					else
					{
						patch = getPatch( source.length() == 0 ? null : source, target );
						Assert.isTrue( patch != null, "Undefined upgrade block found: \"" + source + "\" --> \"" + target + "\"" );
						Assert.isTrue( patch.getType() == type, "Upgrade block type '" + action + "' is different from definition", pos );
					}
					patch.setPos( pos );
				}
			}

			pos = this.file.getLineNumber();
			line = this.file.readLine();
		}
	}

	/**
	 * Returns the patch belonging to the specified source and target. Also check for duplicates.
	 * 
	 * @param source
	 * @param target
	 * @return
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
					Assert.isTrue( result == null, "Duplicate upgrade block found", patch.getPos() );
					result = patch;
				}
			}

		return result;
	}

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
					Assert.isTrue( result == null, "Duplicate init block found", patch.getPos() );
					result = patch;
				}
			}

		return result;
	}

	/**
	 * Returns a patch path for the specified source and target.
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	protected List getPatchPath( String source, String target, boolean downgradeable )
	{
		// If equal than we are finished
		// TODO Make source always not null?
		if( source == null )
		{
			if( target == null )
				return Collections.EMPTY_LIST;
		}
		else
		{
			if( source.equals( target ) )
				return Collections.EMPTY_LIST;
		}

		// Start with all the patches that start with the given source
		List< Patch > patches = (List)this.patches.get( source );
		if( patches == null )
			return null;

		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		// Depth first normal upgrades
		for( Patch patch : patches )
			if( patch.isUpgrade() )
			{
				// Recurse
				List patches2 = getPatchPath( patch.getTarget(), target, downgradeable );
				if( patches2 != null )
				{
					// Found complete path
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
				// Target not reached
			}

		// Depth first only switches
		for( Patch patch : patches )
			if( patch.isSwitch() )
			{
				// Recurse
				List patches2 = getPatchPath( patch.getTarget(), target, downgradeable );
				if( patches2 != null )
				{
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
				// Target not reached
			}

		// Depth first only downgrades
		if( downgradeable )
			for( Patch patch : patches )
				if( patch.isDowngrade() )
				{
					// Recurse
					List patches2 = getPatchPath( patch.getTarget(), target, downgradeable );
					if( patches2 != null )
					{
						List result = new ArrayList();
						result.add( patch );
						result.addAll( patches2 );
						return result;
					}
					// Target not reached
				}

		return null;
	}

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
	 * @param targeting Specifies the direction that has been initiated.
	 * @param tips Only return tip version.
	 * @param prefix Only consider versions that start with the given prefix.
	 * @param result All results are added to this set.
	 */
	protected void collectTargets( String version, String targeting, boolean tips, boolean downgrades, String prefix, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		collectReachableVersions( version, targeting, downgrades, result );

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
								iterator.remove();
			}
	}

	protected LinkedHashSet< String > getReachableVersions( String version, String targeting, boolean downgrades )
	{
		LinkedHashSet result = new LinkedHashSet();
		collectReachableVersions( version, targeting, downgrades, result );
		return result;
	}

	protected void collectReachableVersions( String version, String targeting, boolean downgrades, Set< String > result )
	{
		if( targeting == null && version != null )
			result.add( version );

		List< Patch > patches = (List)this.patches.get( version ); // Get all patches with the given source
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
				if( downgrades || !patch.isDowngrade() ) // Downgrades?
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
	 * @param patch
	 */
	protected void gotoPatch( Patch patch )
	{
		Assert.isTrue( patch.getPos() >= 0, "Upgrade or init block not found" );

		try
		{
			this.file.gotoLine( patch.getPos() );
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
	 * Reads a statement from the patch file or null when no more statements are available in the current patch.
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected Command readStatement()
	{
		StringBuilder result = new StringBuilder();

		while( true )
		{
			try
			{
				String line = this.file.readLine();
				Assert.isTrue( line != null, "Premature end of file found" );

				if( line.trim().length() > 0 )
				{
					if( goPattern.matcher( line ).matches() )
						return new Command( result.toString(), false );

					if( patchEndPattern.matcher( line ).matches() )
					{
						if( result.length() > 0 )
							throw new NonTerminatedStatementException();
						return null;
					}

					if( line.startsWith( "--*" ) )
					{
						if( result.length() > 0 )
							throw new NonTerminatedStatementException();

						if( !patchStartPattern.matcher( line ).matches() ) // skip patch start
						{
							line = line.substring( 3 ).trim();
							if( !line.startsWith( "//" )) // skip comment
								return new Command( line, true );
						}
					}
					else
					{
						result.append( line );
						result.append( "\n" );
					}
				}
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
	}


	public String getEncoding()
	{
		return this.file.getEncoding();
	}
}
