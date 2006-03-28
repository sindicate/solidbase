package ronnie.dbpatcher;

import java.util.Iterator;
import java.util.List;

public class Main
{
	public static void main( String[] args )
	{
		try
		{
			PatchFile.open();
			try
			{
				PatchFile.read();
				
				String version = DBVersion.getVersion();
				System.out.println( "Current version = " + version );
				
				System.out.println( "Targets:" );
				List targets = PatchFile.getTargets( version );
				for( Iterator iter = targets.iterator(); iter.hasNext(); )
				{
					String target = (String)iter.next();
					System.out.println( target );
				}
				
				Database.patch( version, "DHL TTS 2.0.1" );
//				Database.patch( version, "DHL TTS 2.0.11" );
//				Database.patch( "DHL TTS 2.0.9", "DHL TTS 3.0.3" );
				
				System.out.println( "Current version = " + DBVersion.getVersion() );
				System.out.println( "Current target = " + DBVersion.getTarget() );
				System.out.println( "Current statements = " + DBVersion.getStatements() );
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
