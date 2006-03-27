package ronnie.dbpatcher;

public class Main
{
	public static void main( String[] args )
	{
		try
		{
			PatchFile.read();
			
			String version = DBVersion.getVersion();
			System.out.println( "Current version = " + version );
			
//			System.in.read();
		}
		catch( Exception e )
		{
			e.printStackTrace( System.err );
		}
	}
}
