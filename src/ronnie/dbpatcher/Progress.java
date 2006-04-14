package ronnie.dbpatcher;

import ronnie.dbpatcher.core.ProgressListener;
import ronnie.dbpatcher.core.Command;

public class Progress extends ProgressListener
{
	protected Command currentCommand;
	
	protected void openingPatchFile( String patchFile )
	{
		System.out.println( "Opening patchfile: " + patchFile );
	}
	
	protected void patchStarting( String source, String target )
	{
		System.out.print( "Patching \"" + source + "\" to \"" + target + "\"" );
	}

	protected void executing( Command command )
	{
		this.currentCommand = command;
	}
	
	protected void executed()
	{
		System.out.print( "." );
	}

	protected void patchFinished()
	{
		System.out.println();
	}
}
