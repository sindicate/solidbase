package ronnie.dbpatcher.core;

/**
 * 
 * 
 * @author R.M. de Bloois
 * @since Apr 14, 2006
 */
public class ProgressListener
{
	protected void patchStarting( String source, String target )
	{
		// could be implemented in subclass
	}
	
	protected void executing( Command command, String message )
	{
		// could be implemented in subclass
	}

	protected void executed()
	{
		// could be implemented in subclass
	}
	
	protected void patchFinished()
	{
		// could be implemented in subclass
	}
	
	protected void openingPatchFile( String patchFile )
	{
		// could be implemented in subclass
	}

	protected String requestPassword( String user )
	{
		// could be implemented in subclass
		return null;
	}

	protected void skipped( Command command )
	{
		// could be implemented in subclass
	}
	
	protected void exception( Command command )
	{
		// could be implemented in subclass
	}
}
