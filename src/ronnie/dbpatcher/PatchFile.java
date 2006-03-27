package ronnie.dbpatcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

public class PatchFile
{
	static protected Patch initPatch;
	static protected HashMap patches = new HashMap();

	static protected void read() throws IOException
	{
		BufferedReader in = new BufferedReader( new FileReader( "dbpatch.sql" ) );
		try
		{
			boolean withinDefinition = false;
			boolean definitionComplete = false;
			while( !definitionComplete )
			{
				String line = in.readLine();
				Assert.check( line != null, "End-of-file found before reading a complete definition" );
				
				if( line.length() > 0 )
				{
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
						
						Pattern pattern = Pattern.compile( "(PATCH|BRANCH|RETURN) +source=\"([^\"]+)\" +target=\"([^\"]+)\" +description=\"([^\"]+)\"( +open=\"(true)\")?" );
						Matcher matcher = pattern.matcher( line );
						Assert.check( matcher.matches(), "Line should match the following syntax: <(PATCH|BRANCH|RETURN) source=\"...\" target=\"...\" description=\"...\" (open=\"true\")/>" );
						String action = matcher.group( 1 );
						String source = matcher.group( 2 );
						String target = matcher.group( 3 );
						String description = matcher.group( 4 );
						boolean open = "true".equals( matcher.group( 6 ) );
						boolean branch = "BRANCH".equals( action );
						boolean returnBranch = "RETURN".equals( action );
						Patch patch = new Patch( source, target, description, open, branch, returnBranch );
						patches.put( source, patch );
					}
					else if( line.matches( "INIT +.*" ) )
					{
						Assert.check( withinDefinition, "Not within the definition" );
//						System.out.println( "patch" );
						
						Pattern pattern = Pattern.compile( "INIT +target=\"([^\"]+)\"" );
						Matcher matcher = pattern.matcher( line );
						Assert.check( matcher.matches(), "Line should match the following syntax: INIT target=\"...\"" );
						String target = matcher.group( 1 );
						PatchFile.initPatch = new Patch( null, target, null, false, false, false );
					}
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
			
			scan( in );
		}
		finally
		{
			in.close();
		}
	}
	
	static protected void scan( BufferedReader in ) throws IOException
	{
		String line = in.readLine();
		while( line != null )
		{
			if( line.startsWith( "--*" ) )
			{
				if( line.matches( "--\\* *INIT" ) )
				{
					System.out.println( line );
				}
				else if( line.matches( "--\\* (PATCH|BRANCH|RETURN).*" ) )
				{
					System.out.println( line );
				}
			}
			line = in.readLine();
		}
	}
}
