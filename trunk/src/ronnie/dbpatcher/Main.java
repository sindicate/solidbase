package ronnie.dbpatcher;

import java.util.Iterator;
import java.util.List;

import ronnie.dbpatcher.core.Patcher;

import com.cmg.pas.util.Assert;

public class Main
{
	static protected void printCurrentVersion()
	{
		String version = Patcher.getCurrentVersion();
		String target = Patcher.getCurrentTarget();
		int statements = Patcher.getCurrentStatements();
		
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
			System.out.println();
			System.out.println( "DBPatcher v" + Configuration.getVersion() + ", (C) 2006 R.M. de Bloois, LogicaCMG" );
			System.out.println();
			
			Patcher.setDatabase( Configuration.getDriver(), Configuration.getDBUrl() );

			printCurrentVersion();
			
			Patcher.openPatchFile();
			try
			{
				Patcher.readPatchFile();
			
				System.out.print( "Possible targets are: " );
				List targets = Patcher.getTargets();
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

//				System.out.println( "You requested target " + input + "." );
				
				Patcher.patch( input );
				
				System.out.println();
				printCurrentVersion();
			}
			finally
			{
				Patcher.closePatchFile();
			}
		}
		catch( Exception e )
		{
			e.printStackTrace( System.err );
		}
	}
}
