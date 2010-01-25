package solidbase.maven;

import org.apache.maven.plugin.logging.Log;
import solidbase.config.ConfigListener;
import solidbase.core.Command;
import solidbase.core.Patch;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;

import java.io.File;
import java.net.URL;

/**
 * @author Ruud de Jong
 */
public class Progress extends ProgressListener implements ConfigListener {
	private Log log;
	private StringBuilder buffer;

	public Progress( Log log )
	{
		this.log = log;
	}

	void flush()
	{
		if( this.buffer != null && this.buffer.length() > 0 )
		{
			this.log.info( this.buffer.toString() );
			this.buffer = null;
		}
	}

	void info( String message )
	{
		flush();
		this.log.info( message );
	}

	void verbose( String message )
	{
		flush();
		this.log.debug( message );
	}

	public void readingPropertyFile( String path )
	{
		verbose( "Reading property file " + path );
	}

	@Override
	protected void openingPatchFile( File patchFile )
	{
		info( "Opening file '" + patchFile + "'" );
	}

	@Override
	protected void openingPatchFile( URL patchFile )
	{
		info( "Opening file '" + patchFile + "'" );
	}

	@Override
	public void openedPatchFile( PatchFile patchFile )
	{
		info( "    Encoding is '" + patchFile.getEncoding() + "'" );
	}

	@Override
	protected void patchStarting( Patch patch )
	{
		flush();
		switch( patch.getType() )
		{
			case INIT:
				this.buffer = new StringBuilder( "Initializing" );
				break;
			case UPGRADE:
				this.buffer = new StringBuilder( "Upgrading" );
				break;
			case SWITCH:
				this.buffer = new StringBuilder( "Switching" );
				break;
			case DOWNGRADE:
				this.buffer = new StringBuilder( "Downgrading" );
				break;
		}
		if( patch.getSource() == null )
			this.buffer.append( " to \"" + patch.getTarget() + "\"" );
		else
			this.buffer.append( " \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
	}

	@Override
	protected void executing( Command command, String message )
	{
		if( message != null ) // Message can be null, when a message has not been set, but sql is still being executed
		{
			flush();
			this.buffer = new StringBuilder( message );
		}
	}

	@Override
	protected void exception( Command command )
	{
		// The sql is printed by the SQLExecutionException.printStackTrace().
	}

	@Override
	protected void executed()
	{
		if( this.buffer == null )
			this.buffer = new StringBuilder();
		this.buffer.append( '.' );
	}

	@Override
	protected void patchFinished()
	{
		flush();
	}

	@Override
	protected void patchingFinished()
	{
		info( "The database is upgraded." );
	}

	@Override
	protected String requestPassword( String user )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void debug( String message )
	{
		verbose( "DEBUG: " + message );
	}
}
