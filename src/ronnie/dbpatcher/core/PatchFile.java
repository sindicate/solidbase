package ronnie.dbpatcher.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.io.LineInputStream;
import com.logicacmg.idt.commons.util.Assert;

/**
 * This class manages the patch file.
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile
{
	static protected final Pattern patchDefinitionMarkerPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchDefinitionPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	private static final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) [OPEN] \"...\" --> \"...\"";

	static protected final Pattern patchStartMarkerPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static protected final Pattern patchStartPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_START_SYNTAX_ERROR = "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) source=\"...\" target=\"...\"";

	static protected final Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

	static protected final Pattern goPattern = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );

	protected MultiValueMap patches = new MultiValueMap();
	protected LineInputStream lis;

	protected PatchFile( LineInputStream lis )
	{
		this.lis = lis;
	}

	protected void close() throws IOException
	{
		this.lis.close();
		this.lis = null;
	}

	protected void read() throws IOException
	{
		boolean withinDefinition = false;
		boolean definitionComplete = false;
		while( !definitionComplete )
		{
			byte[] bytes = this.lis.readLine();
			Assert.isTrue( bytes != null, "End-of-file found before reading a complete definition" );

			if( bytes.length > 0 )
			{
				String line = new String( bytes );

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
	 * Scans for patches in the file.
	 * 
	 * @throws IOException
	 */
	protected void scan() throws IOException
	{
		long pos = this.lis.getPosition();
		byte[] bytes = this.lis.readLine();

		while( bytes != null )
		{
			String line = new String( bytes );

			if( line.startsWith( "--*" ) )
			{
				if( patchStartMarkerPattern.matcher( line ).matches() )
				{
					Matcher matcher = patchStartPattern.matcher( line );
					Assert.isTrue( matcher.matches(), PATCH_START_SYNTAX_ERROR );
					//					String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 3 );
					//					boolean branch = "BRANCH".equalsIgnoreCase( action );
					//					boolean returnBranch = "RETURN".equalsIgnoreCase( action );
					//					boolean init = "INIT".equalsIgnoreCase( action );

					Patch patch = getPatch( source, target );
					Assert.isTrue( patch != null, "Patch block found for undefined patch: \"" + source + "\" --> \"" + target + "\"" );
					// TODO: Assert that action is the same
					patch.setPos( pos );
				}
			}

			pos = this.lis.getPosition();
			bytes = this.lis.readLine();
		}
	}

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

	protected List getPatches( String version, String target )
	{
		Assert.isTrue( !target.equals( version ), "Target [" + target + "] == version [" + version + "]" );

		List patches = (List)this.patches.get( version );
		if( patches == null )
			return Collections.EMPTY_LIST;
		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			if( patch.getTarget().equals( target ) )
			{
				// Found it
				List result = new ArrayList();
				result.add( patch );
				return result;
			}

			if( !patch.isBranch() )
			{
				// Try recursive through the normal path
				List patches2 = getPatches( patch.getTarget(), target );
				if( patches2.size() > 0 )
				{
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
			}
		}

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			if( patch.isBranch() )
			{
				// Try recursive through the branches
				List patches2 = getPatches( patch.getTarget(), target );
				if( patches2.size() > 0 )
				{
					List result = new ArrayList();
					result.add( patch );
					result.addAll( patches2 );
					return result;
				}
			}
		}

		return Collections.EMPTY_LIST;
	}

	protected void getTargets( String version, String targeting, LinkedHashSet result )
	{
		Assert.notNull( result, "'result' must not be null" );

		List patches = (List)this.patches.get( version );
		if( patches == null )
			return;

		Assert.isTrue( patches.size() > 0, "Not expecting an empty list" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();

			String target = patch.getTarget();
			if( targeting == null || targeting.equals( target ) )
			{
				result.add( target );
				if( !patch.isOpen() )
				{
					// Recursively determine more patches
					getTargets( target, null, result );
				}
			}
		}
	}

	protected void gotoPatch( Patch patch )
	{
		Assert.isTrue( patch.getPos() >= 0, "Patch block not found" );

		try
		{
			this.lis.setPosition( patch.getPos() );
			byte[] bytes = this.lis.readLine();
			String line = new String( bytes );
			//			System.out.println( line );
			Assert.isTrue( patchStartMarkerPattern.matcher( line ).matches() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	protected Command readStatement()
	{
		StringBuilder result = new StringBuilder();

		while( true )
		{
			byte[] bytes;
			try
			{
				bytes = this.lis.readLine();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}

			Assert.isTrue( bytes != null, "Premature end of file found" );

			String line = new String( bytes );
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
	}
}
