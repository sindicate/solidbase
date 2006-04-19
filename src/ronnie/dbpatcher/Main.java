package ronnie.dbpatcher;

import java.util.Iterator;
import java.util.List;

import ronnie.dbpatcher.core.Patcher;

import com.cmg.pas.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
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
			System.out.println( "Current database version is \"" + version + "\", incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
		else
			System.out.println( "Current database version is \"" + version + "\"." );
	}
	
	static protected String list( List list )
	{
		StringBuffer buffer = new StringBuffer();
		
		boolean first = true;
		for( Iterator iter = list.iterator(); iter.hasNext(); )
		{
			Object object = iter.next();
			if( first )
				first = false;
			else
				buffer.append( ", " );
			buffer.append( object );
		}
		
		return buffer.toString();
	}
	
	static public void main( String[] args )
	{
		System.out.println();
		System.out.println( "DBPatcher v" + Configuration.getVersion() + " for Oracle, patch engine v" + Patcher.getVersion() );
		System.out.println( "(C) 2006 R.M. de Bloois, LogicaCMG" );
		System.out.println();

		Patcher.setCallBack( new Progress() );
		
		Patcher.setConnection( Configuration.getDriver(), Configuration.getDBUrl() );
		System.out.println( "Connecting to database..." );
		printCurrentVersion();
		
		if( args.length == 1 && args[ 0 ].equals( "exportlog" ) )
		{
			Patcher.logToXML( System.out );
			return;
		}
		
		try
		{
			Patcher.openPatchFile();
			try
			{
				Patcher.readPatchFile();

				System.out.println( "Possible targets are: " + list( Patcher.getTargets() ) );
				System.out.print( "Input target version: " );

				byte[] buffer = new byte[ 32 ];
				int read = System.in.read( buffer );
				Assert.check( read < 32, "Input too long" );
				Assert.check( buffer[ --read ] == '\n' );
				if( buffer[ read - 1 ] == '\r' )
					read--;

				String input = new String( buffer, 0, read );
				Assert.check( input.length() > 0, "Input too short" );

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
			System.out.println();
			e.printStackTrace( System.out );
		}
	}
}
