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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;


/**
 * This class manages the patch file contents and the paths between versions.
 * 
 * @author Ren� M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile
{
	static protected final Pattern encodingPattern = Pattern.compile( "^--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\"[ \t]*$", Pattern.CASE_INSENSITIVE );

	static protected final Pattern patchDefinitionMarkerPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchDefinitionPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	private static final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) [OPEN] \"...\" --> \"...\"";

	static protected final Pattern patchStartMarkerPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchStartPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) \"...\" --> \"...\"";

	static protected final Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

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
				//					System.out.println( line );
				if( line.equalsIgnoreCase( "PATCHES" ) )
				{
					Assert.isTrue( !withinDefinition, "Already within the definition" );
					withinDefinition = true;
					//					System.out.println( "start" );
				}
				else if( line.startsWith( "//" ) )
				{
					// ignore line
				}
				else if( patchDefinitionMarkerPattern.matcher( line ).matches() )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );
					//						System.out.println( "patch" );

					Matcher matcher = patchDefinitionPattern.matcher( line );
					Assert.isTrue( matcher.matches(), PATCH_DEFINITION_SYNTAX_ERROR );
					String action = matcher.group( 1 );
					boolean open = matcher.group( 2 ) != null;
					String source = matcher.group( 3 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 4 );
					boolean branch = "BRANCH".equalsIgnoreCase( action );
					boolean returnBranch = "RETURN".equalsIgnoreCase( action );
					boolean init = "INIT".equalsIgnoreCase( action );
					Patch patch = new Patch( source, target, branch, returnBranch, open, init );
					if( init )
						this.inits.put( source, patch );
					else
						this.patches.put( source, patch );
				}
				else if( line.equalsIgnoreCase( "/PATCHES" ) )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );
					//					System.out.println( "end" );
					definitionComplete = true;
				}
				else
					throw new SystemException( "Unexpected line within definition: " + line );
			}
		}

		scan();
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
					// boolean branch = "BRANCH".equalsIgnoreCase( action );
					// boolean returnBranch = "RETURN".equalsIgnoreCase( action );
					boolean init = "INIT".equalsIgnoreCase( action );
					Patch patch;
					if( init )
						patch = getInitPatch( source.length() == 0 ? null : source, target );
					else
						patch = getPatch( source.length() == 0 ? null : source, target );
					Assert.isTrue( patch != null, "Patch block found for undefined patch: \"" + source + "\" --> \"" + target + "\"" );
					// TODO Assert that action is the same, or remove this
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
					Assert.isTrue( result == null, "Patch definitions are not unique" );
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
					Assert.isTrue( result == null, "Patch definitions are not unique" );
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
	protected List getPatchPath( String source, String target )
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
		List patches = (List)this.patches.get( source );
		if( patches == null )
			return null;

		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		// Depth first no branches
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			if( !patch.isBranch() )
			{
				// Recurse
				List patches2 = getPatchPath( patch.getTarget(), target );
				if( patches2 != null )
				{
					// Found complete path
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
				// TODO Why no else?
			}
		}

		// Branches
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			if( patch.isBranch() )
			{
				// Try recursive through the branches
				List patches2 = getPatchPath( patch.getTarget(), target );
				if( patches2 != null )
				{
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
				// TODO Why no else?
			}
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
	protected void collectTargets( String version, String targeting, boolean tips, String prefix, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		if( targeting == null && version != null && ( prefix == null || version.startsWith( prefix ) ) )
			result.add( version );

		collectTargets0( version, targeting, tips, prefix, result );
	}

	protected void collectTargets0( String version, String targeting, boolean tips, String prefix, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		List patches = (List)this.patches.get( version );
		if( patches == null )
			return;
		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();

			// When already targeting a specific version, ignore the rest.
			if( targeting == null || targeting.equals( patch.getTarget() ) )
			{
				// Don't add targets that don't start with the given prefix.
				if( prefix == null || patch.getTarget().startsWith( prefix ) )
				{
					// Normal patch --> the source version is not a tip version. Remove it.
					if( tips && !patch.isReturnBranch() )
						result.remove( patch.getSource() );
					result.add( patch.getTarget() );
				}
				if( !patch.isOpen() )
					collectTargets0( patch.getTarget(), null, tips, prefix, result ); // Recursively determine more patches
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
		Assert.isTrue( patch.getPos() >= 0, "Patch block not found" );

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