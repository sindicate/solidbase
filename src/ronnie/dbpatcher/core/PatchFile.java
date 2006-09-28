package ronnie.dbpatcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;

import com.lcmg.rbloois.SystemException;
import com.lcmg.rbloois.io.LineInputStream;
import com.lcmg.rbloois.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchFile
{
	static protected MultiValueMap patches = new MultiValueMap();
	static protected LineInputStream lis;
	static protected Pattern patchDefinitionMarkerPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static protected Pattern patchDefinitionPattern = Pattern.compile( "(INIT|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	static protected Pattern patchStartMarkerPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static protected Pattern patchStartPattern = Pattern.compile( "--\\*[ \t]*(INIT|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );
	static protected Pattern patchEndPattern = Pattern.compile( "--\\* */(INIT|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );
	static protected Pattern goPattern = Pattern.compile( "GO *", Pattern.CASE_INSENSITIVE );

	static protected void open() throws IOException
	{
		URL url = PatchFile.class.getResource( "/dbpatch.sql" );
		if( url != null )
		{
			lis = new LineInputStream( url );
			Patcher.callBack.openingPatchFile( "Opening patchfile: " + url );
		}
		else
		{
			File file = new File( "dbpatch.sql" );
			lis = new LineInputStream( new FileInputStream( file ) );
			Patcher.callBack.openingPatchFile( "Opening patchfile: " + file.getAbsolutePath() );
		}
	}
	
	static protected void close() throws IOException
	{
		lis.close();
		lis = null;
	}
	
	static protected void read() throws IOException
	{
		boolean withinDefinition = false;
		boolean definitionComplete = false;
		while( !definitionComplete )
		{
			byte[] bytes = lis.readLine();
			Assert.check( bytes != null, "End-of-file found before reading a complete definition" );
			
			if( bytes.length > 0 )
			{
				String line = new String( bytes );
				
				Assert.check( line.startsWith( "--*" ), "Line should start with --*" );
				line = line.substring( 3 ).trim();
//					System.out.println( line );
				if( line.equalsIgnoreCase( "PATCHES" ) )
				{
					Assert.check( !withinDefinition, "Already within the definition" );
					withinDefinition = true;
//					System.out.println( "start" );
				}
				else if( line.startsWith( "//" ) )
				{
					// ignore line
				}
				else if( patchDefinitionMarkerPattern.matcher( line ).matches() )
				{
					Assert.check( withinDefinition, "Not within the definition" );
//						System.out.println( "patch" );
					
					Matcher matcher = patchDefinitionPattern.matcher( line );
					Assert.check( matcher.matches(), "Line should match the following syntax: (INIT|PATCH|BRANCH|RETURN) [OPEN] \"...\" --> \"...\"" );
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
					patches.put( source, patch );
				}
				else if( line.equalsIgnoreCase( "/PATCHES" ) )
				{
					Assert.check( withinDefinition, "Not within the definition" );
//					System.out.println( "end" );
					definitionComplete = true;
				}
				else
					throw new SystemException( "Unexpected line within definition: " + line );
			}
		}
		
		scan();
	}
	
	static protected void scan() throws IOException
	{
		long pos = lis.getPosition();
		byte[] bytes = lis.readLine();
		
		while( bytes != null )
		{
			String line = new String( bytes );
			
			if( line.startsWith( "--*" ) )
			{
				if( patchStartMarkerPattern.matcher( line ).matches() )
				{
					Matcher matcher = patchStartPattern.matcher( line );
					Assert.check( matcher.matches(), "Line should match the following syntax: (PATCH|BRANCH|RETURN) source=\"...\" target=\"...\"" );
//					String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 3 );
//					boolean branch = "BRANCH".equalsIgnoreCase( action );
//					boolean returnBranch = "RETURN".equalsIgnoreCase( action );
//					boolean init = "INIT".equalsIgnoreCase( action );
					
					Patch patch = getPatch( source, target );
					Assert.check( patch != null, "Patch block found for undefined patch: \"" + source + "\" --> \"" + target + "\"" );
					// TODO: Assert that action is the same
					patch.setPos( pos );
				}
			}
			
			pos = lis.getPosition();
			bytes = lis.readLine();
		}
	}
	
	static protected Patch getPatch( String source, String target )
	{
		Patch result = null;

		List patches = (List)PatchFile.patches.get( source );
		if( patches != null )
			for( Iterator iter = patches.iterator(); iter.hasNext(); )
			{
				Patch patch = (Patch)iter.next();
				if( patch.getTarget().equals( target ) )
				{
					Assert.check( result == null, "Patch definitions are not unique" );
					result = patch;
				}
			}

		return result;
	}

	static protected List getPatches( String version, String target )
	{
		List result = new ArrayList();

		while( version == null || !version.equals( target ) )
		{
			List patches = (List)PatchFile.patches.get( version );
			if( patches == null )
				return Collections.EMPTY_LIST;
			
			if( patches.size() > 1 )
			{
				// branches should go last
				for( Iterator iter = patches.iterator(); iter.hasNext(); )
				{
					Patch patch = (Patch)iter.next();
					if( !patch.isBranch() )
					{
						List patches2 = getPatches( patch.getTarget(), target );
						if( patches2.size() > 0 )
						{
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
						List patches2 = getPatches( patch.getTarget(), target );
						if( patches2.size() > 0 )
						{
							result.add( patch );
							result.addAll( patches2 );
							return result;
						}
					}
				}
				
				return Collections.EMPTY_LIST;
			}
			
			Patch patch = (Patch)patches.get( 0 ); 
			result.add( patch );
			version = patch.getTarget();
		}
		
		return result;
	}

	static protected List getTargets( String version )
	{
		LinkedHashSet result = new LinkedHashSet();

		while( true )
		{
			List patches = (List)PatchFile.patches.get( version );
			if( patches == null )
				break;
			
			if( patches.size() > 1 )
			{
				for( Iterator iter = patches.iterator(); iter.hasNext(); )
				{
					Patch patch = (Patch)iter.next();
					version = patch.getTarget();
					result.add( version );
					if( !patch.isOpen() )
					{
						List patches2 = getTargets( version );
						if( patches2 != null )
							result.addAll( patches2 );
					}
				}
				
				break;
			}
			
			Patch patch = (Patch)patches.get( 0 );
			version = patch.getTarget();
			result.add( version );
			
			if( patch.isOpen() )
				break;
		}
		
		return new ArrayList( result );
	}

	static protected void gotoPatch( Patch patch )
	{
		Assert.check( patch.getPos() >= 0, "Patch block not found" );
		
		try
		{
			lis.setPosition( patch.getPos() );
			byte[] bytes = lis.readLine();
			String line = new String( bytes );
//			System.out.println( line );
			Assert.check( patchStartMarkerPattern.matcher( line ).matches() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
	
	static protected Command readStatement()
	{
		StringBuilder result = new StringBuilder();
		
		while( true )
		{
			byte[] bytes;
			try
			{
				bytes = lis.readLine();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
			
			Assert.check( bytes != null, "Premature end of file found" );
			
			String line = new String( bytes );
			if( line.trim().length() > 0 )
			{
				if( goPattern.matcher( line ).matches() )
					return new Command( result.toString(), true );
				
				if( patchEndPattern.matcher( line ).matches() )
				{
					Assert.check( result.length() == 0, "Unterminated statement found" );
					return null;
				}
				
				if( line.startsWith( "--*" ) )
				{
					line = line.substring( 3 ).trim();
					if( !line.startsWith( "//" ))
						return new Command( line, false );
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
