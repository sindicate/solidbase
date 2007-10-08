package test;

import ronnie.dbpatcher.core.Command;
import ronnie.dbpatcher.core.ProgressListener;

public class TestProgressListener extends ProgressListener
{

	@Override
	protected void debug( String message )
	{
		System.out.println( "DEBUG: " + message );
	}

	@Override
	protected void exception( Command command )
	{
		System.out.println( "EXCEPTION: " + command );
	}

	@Override
	protected void executed()
	{
		System.out.println( "EXECUTED." );
	}

	@Override
	protected void executing( Command command, String message )
	{
		System.out.println( "EXECUTING: " + message );
	}

	@Override
	protected void openingPatchFile( String patchFile )
	{
		System.out.println( "OPENINGPATCHFILE: " + patchFile );
	}

	@Override
	protected void patchFinished()
	{
		System.out.println( "PATCHFINISHED." );
	}

	@Override
	protected void patchStarting( String source, String target )
	{
		System.out.println( "PATCHSTARTING: " + source + " - " + target );
	}

	@Override
	protected String requestPassword( String user )
	{
		System.out.println( "REQUESTPASSWORD: " + user );
		return null;
	}

	@Override
	protected void skipped( Command command )
	{
		System.out.println( "SKIPPED." );
	}
}
