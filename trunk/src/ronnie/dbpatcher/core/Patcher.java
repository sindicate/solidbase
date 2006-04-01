package ronnie.dbpatcher.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Patcher
{
	static public void openPatchFile() throws IOException
	{
		PatchFile.open();
	}
	
	static public void readPatchFile() throws IOException
	{
		PatchFile.read();
	}
	
	static public void closePatchFile() throws IOException
	{
		PatchFile.close();
	}
	
	static public String getCurrentVersion()
	{
		return DBVersion.getVersion();
	}
	
	static public String getCurrentTarget()
	{
		return DBVersion.getTarget();
	}
	
	static public int getCurrentStatements()
	{
		return DBVersion.getStatements();
	}
	
	static public List getTargets()
	{
		return 	PatchFile.getTargets( DBVersion.getVersion() );
	}
	
	static public void patch( String target ) throws SQLException
	{
		Database.patch( DBVersion.getVersion(), target );
	}

	public static void setDatabase( String driverName, String url )
	{
		Database.setDatabase( driverName, url );
	}
}
