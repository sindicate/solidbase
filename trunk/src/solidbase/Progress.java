/*--
 * Copyright 2006 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase;

import java.io.File;
import java.net.URL;

import solidbase.config.ConfigListener;
import solidbase.core.Command;
import solidbase.core.Patch;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;


/**
 * Implements the progress listener for the command line version of SolidBase.
 * 
 * @author René M. de Bloois
 */
public class Progress extends ProgressListener implements ConfigListener
{
	/**
	 * Show extra information?
	 */
	protected boolean verbose;

	/**
	 * The console to use.
	 */
	protected Console console;


	/**
	 * Constructor.
	 * 
	 * @param console The console to use.
	 * @param verbose Show extra information?
	 */
	public Progress( Console console, boolean verbose )
	{
		this.console = console;
		this.verbose = verbose;
	}

	public void readingConfigFile( String path )
	{
		if( this.verbose )
			this.console.println( "Reading property file " + path );
	}

	@Override
	protected void openingPatchFile( File patchFile )
	{
		this.console.println( "Opening file '" + patchFile + "'" );
	}

	@Override
	protected void openingPatchFile( URL patchFile )
	{
		this.console.println( "Opening file '" + patchFile + "'" );
	}

	@Override
	public void openedPatchFile( PatchFile patchFile )
	{
		this.console.println( "    Encoding is '" + patchFile.getEncoding() + "'" );
	}

	@Override
	protected void patchStarting( Patch patch )
	{
		switch( patch.getType() )
		{
			case INIT:
				this.console.print( "Initializing" );
				break;
			case UPGRADE:
				this.console.print( "Upgrading" );
				break;
			case SWITCH:
				this.console.print( "Switching" );
				break;
			case DOWNGRADE:
				this.console.print( "Downgrading" );
				break;
		}
		if( patch.getSource() == null )
			this.console.print( " to \"" + patch.getTarget() + "\"" );
		else
			this.console.print( " \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
	}

	@Override
	protected void executing( Command command, String message )
	{
		if( message != null ) // Message can be null, when a message has not been set, but sql is still being executed
		{
			this.console.carriageReturn();
			this.console.print( message );
		}
	}

	@Override
	protected void exception( Command command )
	{
		// The sql is now printed by the SQLExecutionException.printStackTrace().
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
		this.console.println( "The database is upgraded." );
	}

	@Override
	protected String requestPassword( String user )
	{
		this.console.carriageReturn();
		this.console.print( "Input password for user '" + user + "': " );
		return this.console.input( true );
	}

	@Override
	protected void debug( String message )
	{
		if( this.verbose )
			this.console.println( "DEBUG: " + message );
	}
}
