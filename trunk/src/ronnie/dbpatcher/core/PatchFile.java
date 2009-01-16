package ronnie.dbpatcher.core;

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

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.io.RandomAccessLineReader;
import com.logicacmg.idt.commons.util.Assert;

/**
 * This class manages the patch file contents and the paths between versions.
 * 
 * @author René M. de Bloois
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
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) source=\"...\" target=\"...\"";

	static protected final Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

	static protected final Pattern goPattern = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );

	protected MultiValueMap patches = new MultiValueMap();
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
					Assert.isTrue( matcher.matches(), PATCH_START_SYNTAX_ERROR );
					// String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 3 );
					// boolean branch = "BRANCH".equalsIgnoreCase( action );
					// boolean returnBranch = "RETURN".equalsIgnoreCase( action );
					// boolean init = "INIT".equalsIgnoreCase( action );

					Patch patch = getPatch( source, target );
					Assert.isTrue( patch != null, "Patch block found for undefined patch: \"" + source + "\" --> \"" + target + "\"" );
					// TODO Assert that action is the same
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
			}
		}

		return null;
	}

	/**
	 * Determines all possible target versions from the specified source version.
	 * 
	 * @param version
	 * @param targeting Specifies the direction that has been initiated.
	 * @param tips Only return tip version
	 * @param result
	 */
	protected void collectTargets( String version, String targeting, boolean tips, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		List patches = (List)this.patches.get( version );
		if( patches == null )
			return;
		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			if( targeting == null || targeting.equals( patch.getTarget() ) )
			{
				if( tips && !patch.isReturnBranch() )
					result.remove( patch.getSource() );
				result.add( patch.getTarget() );
				if( !patch.isOpen() )
					collectTargets( patch.getTarget(), null, tips, result ); // Recursively determine more patches
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
						Assert.isTrue( result.length() == 0, "Unterminated statement found" );
						return null;
					}

					if( line.startsWith( "--*" ) )
					{
						line = line.substring( 3 ).trim();
						if( !line.startsWith( "//" ))
							return new Command( line, true );
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
