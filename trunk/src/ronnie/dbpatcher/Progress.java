package ronnie.dbpatcher;

import java.io.IOException;

import ronnie.dbpatcher.core.Command;
import ronnie.dbpatcher.core.ProgressListener;

import com.cmg.pas.SystemException;

public class Progress extends ProgressListener
{
	protected Command currentCommand;
	
	protected void openingPatchFile( String patchFile )
	{
		Console.println( "Opening patchfile: " + patchFile );
	}
	
	protected void patchStarting( String source, String target )
	{
		Console.print( "Patching \"" + source + "\" to \"" + target + "\"" );
	}

	protected void executing( Command command, String message )
	{
		this.currentCommand = command;
		if( message != null )
		{
			Console.carriageReturn();
			Console.print( message );
		}
	}
	
	protected void exception( Command command )
	{
		Console.emptyLine();
		Console.println( "Exception while executing:" );
		Console.println( command.getCommand() );
	}
	
	protected void executed()
	{
		Console.print( "." );
	}

	protected void skipped()
	{
		Console.print( "x" );
	}

	protected void patchFinished()
	{
		Console.println();
	}

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
}
