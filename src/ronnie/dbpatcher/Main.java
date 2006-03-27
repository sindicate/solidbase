package ronnie.dbpatcher;

import java.sql.DriverManager;

public class Main
{
	public static void main( String[] args )
	{
		try
		{
			PatchFile.read();
			
//			connectPatches();
			Class.forName( "org.apache.derby.jdbc.EmbeddedDriver" );
			Database.connection = DriverManager.getConnection( "jdbc:derby:c:/projects/java/dbpatcher/derbyDB;create=true" );
	
			if( !DBVersion.doesTableExist() )
				DBVersion.createTables();
			
			String version = DBVersion.getVersion();
			System.out.println( "Current version = " + version );
			
			System.in.read();
		}
		catch( Exception e )
		{
			e.printStackTrace( System.err );
		}
	}
}
