package ronnie.dbpatcher;

import java.util.Iterator;
import java.util.List;

import com.cmg.pas.util.Assert;

public class Main
{
	static protected void printCurrentVersion()
	{
		String version = DBVersion.getVersion();
		String target = DBVersion.getTarget();
		int statements = DBVersion.getStatements();
		
		if( version == null )
			System.out.println( "The database has no version yet." );
		else if( target != null )
			System.out.println( "Current database version is \"" + version + "\", incompletely patched to version \"" + target + "\" (" + statements + " statements succesful)." );
		else
			System.out.println( "Current database version is \"" + version + "\"." );
		
	}
	
	public static void main( String[] args )
	{
		try
		{
			PatchFile.open();
			try
			{
				PatchFile.read();
			
				printCurrentVersion();
				
				System.out.print( "Possible targets are: " );
				List targets = PatchFile.getTargets( DBVersion.getVersion() );
				boolean first = true;
				for( Iterator iter = targets.iterator(); iter.hasNext(); )
				{
					String target = (String)iter.next();
					if( first )
						first = false;
					else
						System.out.print( ", " );
					System.out.print( target );
				}
				System.out.println();
				
				System.out.print( "Input target version: " );
				byte[] buffer = new byte[ 32 ];
				int read = System.in.read( buffer );
				Assert.check( read < 32, "Input too long" );
				Assert.check( buffer[ read - 1 ] == '\n' );
				String input;
				if( buffer[ read - 2 ] == '\r' )
					input = new String( buffer, 0, read - 2 );
				else
					input = new String( buffer, 0, read - 1 );
				Assert.check( input.length() > 0, "Input too short" );

				System.out.println( "You requested target " + input + "." );
				
				Database.patch( DBVersion.getVersion(), input );
				
				printCurrentVersion();
			}
			finally
			{
				PatchFile.close();
			}
				
		}
		catch( Exception e )
		{
			e.printStackTrace( System.err );
		}
	}
}
