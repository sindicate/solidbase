/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.maven;

import org.apache.maven.plugin.logging.Log;

import solidbase.core.Assert;
import solidbase.core.Command;
import solidbase.core.Patch;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;
import solidbase.core.SQLExecutionException;
import solidbase.core.SQLFile;

import java.io.File;
import java.net.URL;


/**
 * Implements the progress listener for the Maven plugin.
 * 
 * @author Ruud de Jong
 * @author René M. de Bloois
 */
public class Progress extends ProgressListener
{
	static private final String SPACES = "                                        ";

	/**
	 * The Maven log.
	 */
	protected Log log;

	/**
	 * Buffer to collect output before logging.
	 */
	protected StringBuilder buffer;

	/**
	 * A store for nested messages coming from SECTIONs in the command file.
	 */
	protected String[] messages = new String[ 10 ];

	/**
	 * Constructor.
	 * 
	 * @param log The Maven log.
	 */
	public Progress( Log log )
	{
		this.log = log;
	}

	/**
	 * Flush collected output to the Maven log.
	 */
	void flush()
	{
		if( this.buffer != null && this.buffer.length() > 0 )
		{
			this.log.info( this.buffer.toString() );
			this.buffer = null;
		}
	}

	/**
	 * Log an info message to the Maven log.
	 * 
	 * @param message The message to log.
	 */
	void info( String message )
	{
		flush();
		this.log.info( message );
	}

	/**
	 * Log a verbose message to the Maven log.
	 * 
	 * @param message The message to log.
	 */
	void verbose( String message )
	{
		flush();
		this.log.debug( message );
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
	protected void openingSQLFile( File sqlFile )
	{
		info( "Opening file '" + sqlFile + "'" );
	}

	@Override
	protected void openingSQLFile( URL sqlFile )
	{
		info( "Opening file '" + sqlFile + "'" );
	}

	@Override
	protected void openedPatchFile( PatchFile patchFile )
	{
		info( "    Encoding is '" + patchFile.getEncoding() + "'" );
	}

	@Override
	protected void openedSQLFile( SQLFile sqlFile )
	{
		info( "    Encoding is '" + sqlFile.getEncoding() + "'" );
	}

	@Override
	protected void patchStarting( Patch patch )
	{
		flush();
		switch( patch.getType() )
		{
			case SETUP:
				this.buffer = new StringBuilder( "Setting up control tables" );
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
			default:
				Assert.fail( "Unknown patch type: " + patch.getType() );
		}
		if( patch.getSource() == null )
			this.buffer.append( " to \"" + patch.getTarget() + "\"" );
		else
			this.buffer.append( " \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
	}

	@Override
	protected void startSection( int level, String message )
	{
		this.messages[ level ] = message;
	}

	@Override
	protected void executing( Command command, String message )
	{
		for( int i = 0; i < this.messages.length; i++ )
		{
			String m = this.messages[ i ];
			if( m != null )
			{
				flush();
				this.buffer = new StringBuilder().append( SPACES, 0, i * 4 ).append( m );
				this.messages[ i ] = null;
			}
		}

		if( message != null ) // Message can be null, when a message has not been set, but sql is still being executed
		{
			flush();
			this.buffer = new StringBuilder( message );
		}
	}

	@Override
	protected void exception( SQLExecutionException exception )
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
	protected void sqlExecutionComplete()
	{
		info( "Execution complete." );
	}

	@Override
	protected void upgradeComplete()
	{
		info( "The database is upgraded." );
	}

	@Override
	protected String requestPassword( String username )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void debug( String message )
	{
		verbose( "DEBUG: " + message );
	}

	@Override
	public void print( String message )
	{
		flush();
		this.buffer = new StringBuilder( message );
	}
}
