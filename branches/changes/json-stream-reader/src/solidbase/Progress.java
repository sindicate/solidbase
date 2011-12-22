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

import solidbase.config.ConfigListener;
import solidbase.core.Command;
import solidbase.core.ProgressListener;
import solidbase.core.UpgradeSegment;
import solidbase.util.Assert;


/**
 * Implements the progress listener for the command line version of SolidBase.
 *
 * @author René M. de Bloois
 */
public class Progress extends ProgressListener implements ConfigListener
{
	static private final String SPACES = "                                        ";

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

	@Override
	public void cr()
	{
		this.console.carriageReturn();
	}

	@Override
	public void println( String message )
	{
		this.console.println( message );
	}

	public void readingConfigFile( String path )
	{
		if( this.verbose )
			this.console.println( "Reading property file " + path );
	}

	@Override
	protected void upgradeStarting( UpgradeSegment segment )
	{
		switch( segment.getType() )
		{
			case SETUP:
				this.console.print( "Setting up control tables" );
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
			default:
				Assert.fail( "Unknown segment type: " + segment.getType() );
		}
		if( segment.getSource() == null )
			this.console.print( " to \"" + segment.getTarget() + "\"" );
		else
			this.console.print( " \"" + segment.getSource() + "\" to \"" + segment.getTarget() + "\"" );
	}

	@Override
	protected void executing( Command command )
	{
		for( int i = 0; i < this.messages.length; i++ )
		{
			String m = this.messages[ i ];
			if( m != null )
			{
				this.console.carriageReturn();
				this.console.print( SPACES.substring( 0, i * 4 ) );
				this.console.print( m );
				this.messages[ i ] = null;
			}
		}
	}

	@Override
	protected void executed()
	{
		this.console.print( "." );
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

	@Override
	public void print( String message )
	{
		this.console.carriageReturn();
		this.console.print( message );
	}
}
