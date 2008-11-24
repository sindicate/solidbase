package ronnie.dbpatcher;

import java.io.IOException;

import ronnie.dbpatcher.core.Command;
import ronnie.dbpatcher.core.ProgressListener;

import com.logicacmg.idt.commons.SystemException;

public class Progress extends ProgressListener
{
	boolean verbose;

	protected Progress( boolean verbose )
	{
		this.verbose = verbose;
	}

	@Override
	protected void openingPatchFile( String patchFile )
	{
		Console.println( "Opening patchfile '" + patchFile + "'" );
	}

	@Override
	protected void patchStarting( String source, String target )
	{
		Console.print( "Patching \"" + source + "\" to \"" + target + "\"" );
	}

	@Override
	protected void executing( Command command, String message )
	{
		if( message != null )
		{
			Console.carriageReturn();
			Console.print( message );
		}
	}

	@Override
	protected void exception( Command command )
	{
		Console.emptyLine();
		Console.println( "Exception while executing:" );
		Console.println( command.getCommand() );
	}

	@Override
	protected void executed()
	{
		Console.print( "." );
	}

	@Override
	protected void patchFinished()
	{
		Console.println();
	}

	@Override
	protected String requestPassword( String user )
	{
		Console.carriageReturn();
		Console.print( "Input password for user '" + user + "': " );
		try
		{
			return Console.input();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	@Override
	protected void debug( String message )
	{
		if( this.verbose )
			Console.println( "DEBUG: " + message );
	}
}
