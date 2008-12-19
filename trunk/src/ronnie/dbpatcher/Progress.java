package ronnie.dbpatcher;

import java.io.IOException;

import ronnie.dbpatcher.config.ConfigListener;
import ronnie.dbpatcher.core.Command;
import ronnie.dbpatcher.core.ProgressListener;

import com.logicacmg.idt.commons.SystemException;

public class Progress extends ProgressListener implements ConfigListener
{
	protected boolean verbose;
	protected Console console;

	public Progress( Console console, boolean verbose )
	{
		this.console = console;
		this.verbose = verbose;
	}

	public void readingPropertyFile( String path )
	{
		if( this.verbose )
			this.console.println( "Reading property file " + path );
	}

	@Override
	protected void openingPatchFile( String patchFile )
	{
		this.console.println( "Opening patchfile '" + patchFile + "'" );
	}

	@Override
	protected void patchStarting( String source, String target )
	{
		this.console.print( "Patching \"" + source + "\" to \"" + target + "\"" );
	}

	@Override
	protected void executing( Command command, String message )
	{
		if( message != null )
		{
			this.console.carriageReturn();
			this.console.print( message );
		}
	}

	@Override
	protected void exception( Command command )
	{
		this.console.emptyLine();
		this.console.println( "Exception while executing:" );
		this.console.println( command.getCommand() );
	}

	@Override
	protected void executed()
	{
		this.console.print( "." );
	}

	@Override
	protected void patchFinished()
	{
		this.console.println();
	}

	@Override
	protected void patchingFinished()
	{
		this.console.println( "The database has been patched." );
	}

	@Override
	protected String requestPassword( String user )
	{
		this.console.carriageReturn();
		this.console.print( "Input password for user '" + user + "': " );
		try
		{
			return this.console.input( true );
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
			this.console.println( "DEBUG: " + message );
	}
}
