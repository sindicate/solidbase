package ronnie.dbpatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiHashMap;

import com.cmg.pas.SystemException;
import com.cmg.pas.io.LineFileInputStream;
import com.cmg.pas.util.Assert;

public class PatchFile
{
	static protected MultiHashMap patches = new MultiHashMap();
	static protected LineFileInputStream lis;

	static protected void open() throws FileNotFoundException
	{
		lis = new LineFileInputStream( "dbpatch.sql" );
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
				if( line.matches( "DEFINITION" ) )
				{
					Assert.check( !withinDefinition, "Already within the definition" );
					withinDefinition = true;
					System.out.println( "start" );
				}
				else if( line.matches( "(PATCH|BRANCH|RETURN) +.*" ) )
				{
					Assert.check( withinDefinition, "Not within the definition" );
//						System.out.println( "patch" );
					
					Pattern pattern = Pattern.compile( "(PATCH|BRANCH|RETURN) +source=\"([^\"]*)\" +target=\"([^\"]+)\" +description=\"([^\"]+)\"( +open=\"(true)\")?" );
					Matcher matcher = pattern.matcher( line );
					Assert.check( matcher.matches(), "Line should match the following syntax: (PATCH|BRANCH|RETURN) source=\"...\" target=\"...\" description=\"...\" (open=\"true\")" );
					String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 3 );
					String description = matcher.group( 4 );
					boolean open = "true".equals( matcher.group( 6 ) );
					boolean branch = "BRANCH".equals( action );
					boolean returnBranch = "RETURN".equals( action );
					Patch patch = new Patch( source, target, description, open, branch, returnBranch );
					patches.put( source, patch );
				}
//					else if( line.matches( "INIT +.*" ) )
//					{
//						Assert.check( withinDefinition, "Not within the definition" );
////						System.out.println( "patch" );
//						
//						Pattern pattern = Pattern.compile( "INIT +target=\"([^\"]+)\"" );
//						Matcher matcher = pattern.matcher( line );
//						Assert.check( matcher.matches(), "Line should match the following syntax: INIT target=\"...\"" );
//						String target = matcher.group( 1 );
//						PatchFile.initPatch = new Patch( null, target, null, false, false, false );
//					}
				else if( line.matches( "/DEFINITION" ) )
				{
					Assert.check( withinDefinition, "Not within the definition" );
					System.out.println( "end" );
					definitionComplete = true;
				}
				else
					throw new SystemException( "Unexpected line within definition: " + line );
			}
		}
		
		scan( lis );
		
		lis.setPosition( 1600 ); // TODO: Remove test
	}
	
	static protected void scan( LineFileInputStream lis ) throws IOException
	{
		byte[] bytes = lis.readLine();
		while( bytes != null )
		{
			String line = new String( bytes );
			
			if( line.startsWith( "--*" ) )
			{
//				if( line.matches( "--\\* *INIT.*" ) )
//				{
//					System.out.println( line );
//					
//					Pattern pattern = Pattern.compile( "--\\* *INIT +target=\"([^\"]+)\"" );
//					Matcher matcher = pattern.matcher( line );
//					Assert.check( matcher.matches(), "Line should match the following syntax: INIT target=\"...\"" );
//					String target = matcher.group( 1 );
//					
//					Patch patch = getPatch( null, target );
//					Assert.check( patch != null, "Patch block found for undefined patch" );
//					// TODO: Assert that action is the same
//					patch.setPos( lis.getPos() );
//				}
				if( line.matches( "--\\* *(PATCH|BRANCH|RETURN).*" ) )
				{
					System.out.println( line );
					
					Pattern pattern = Pattern.compile( "--\\* *(PATCH|BRANCH|RETURN) +source=\"([^\"]*)\" +target=\"([^\"]+)\"" );
					Matcher matcher = pattern.matcher( line );
					Assert.check( matcher.matches(), "Line should match the following syntax: (PATCH|BRANCH|RETURN) source=\"...\" target=\"...\"" );
					String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 3 );
					boolean branch = "BRANCH".equals( action );
					boolean returnBranch = "RETURN".equals( action );
					
					Patch patch = getPatch( source, target );
					Assert.check( patch != null, "Patch block found for undefined patch" );
					// TODO: Assert that action is the same
					patch.setPos( lis.getPosition() );
				}
			}
			
			bytes = lis.readLine();
		}
	}
	
	static protected Patch getPatch( String source, String target )
	{
		Patch result = null;

		List patches = (List)PatchFile.patches.get( source );
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

//		if( version == null )
//		{
//			if( initPatch == null )
//				return result;
//			result.add( initPatch );
//			version = initPatch.getTarget();
//		}
			
		while( version == null || !version.equals( target ) )
		{
			List patches = (List)PatchFile.patches.get( version );
			if( patches == null )
				return result;
			
			if( patches.size() > 1 )
			{
				// branches should go last
				for( Iterator iter = patches.iterator(); iter.hasNext(); )
				{
					Patch patch = (Patch)iter.next();
					if( !patch.isBranch() )
					{
						List patches2 = getPatches( patch.getTarget(), target );
						if( patches2 != null )
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
						if( patches2 != null )
						{
							result.add( patch );
							result.addAll( patches2 );
							return result;
						}
					}
				}
				return result;
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

//		if( version == null )
//		{
//			if( initPatch == null )
//				return null;
//			version = initPatch.getTarget();
//			result.add( version );
//		}
			
		while( true )
		{
			List patches = (List)PatchFile.patches.get( version );
			if( patches == null )
				return new ArrayList( result );
			
			if( patches.size() > 1 )
			{
				for( Iterator iter = patches.iterator(); iter.hasNext(); )
				{
					Patch patch = (Patch)iter.next();
					version = patch.getTarget();
					result.add( version );
					List patches2 = getTargets( version );
					if( patches2 != null )
						result.addAll( patches2 );
				}
				return new ArrayList( result );
			}
			
			Patch patch = (Patch)patches.get( 0 ); 
			version = patch.getTarget();
			result.add( version );
		}
	}

	static public void gotoPatch( Patch patch )
	{
		Assert.check( patch.getPos() >= 0, "Patch block not found" );
		
		try
		{
			lis.setPosition( patch.getPos() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
	
	static public String readStatement()
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
				if( line.matches( "--\\* *" ) )
				{
					Assert.check( result.length() > 0, "Empty statement found" );
					return result.toString();
				}
				
				if( line.matches( "--\\* */PATCH *" ) )
				{
					Assert.check( result.length() == 0, "Unterminated statement found" );
					return null;
				}
				
				if( !line.startsWith( "--*" ) )
				{
					result.append( line );
					result.append( "\n" );
				}
			}
		}
	}
}
