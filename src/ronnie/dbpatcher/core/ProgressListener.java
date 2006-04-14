package ronnie.dbpatcher.core;

public class ProgressListener
{
	protected void patchStarting( String source, String target )
	{
		// to be overridden
	}
	
	protected void executing( Command command )
	{
		// to be overridden
	}

	protected void executed()
	{
		// TODO Auto-generated method stub
	}
	
	protected void patchFinished()
	{
		// to be overridden
	}
	
	protected void openingPatchFile( String patchFile )
	{
		// to be overridden
	}
}
