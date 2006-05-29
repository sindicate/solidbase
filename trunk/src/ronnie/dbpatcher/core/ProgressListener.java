package ronnie.dbpatcher.core;

public class ProgressListener
{
	protected void patchStarting( String source, String target )
	{
		// to be overridden
	}
	
	protected void executing( Command command, String message )
	{
		// to be overridden
	}

	protected void executed()
	{
		// to be overridden
	}
	
	protected void patchFinished()
	{
		// to be overridden
	}
	
	protected void openingPatchFile( String patchFile )
	{
		// to be overridden
	}

	public String requestPassword( String user )
	{
		// to be overridden
		return null;
	}

	public void skipped( Command command )
	{
		// TODO Auto-generated method stub
	}
}
