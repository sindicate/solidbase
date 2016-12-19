/*--
 * Copyright 2010 Ren� M. de Bloois
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

import solidbase.core.Command;
import solidbase.core.ProgressListener;
import solidbase.core.UpgradeSegment;
import solidbase.util.Assert;


/**
 * Implements the progress listener for the Maven plugin.
 *
 * @author Ruud de Jong
 * @author Ren� M. de Bloois
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
	 * Constructor.
	 *
	 * @param log The Maven log.
	 */
	public Progress( Log log )
	{
		this.log = log;
	}

	@Override
	public void reset()
	{
		super.reset();
		this.buffer = null;
	}

	@Override
	public void cr()
	{
		flush();
	}

	@Override
	public void println( String message )
	{
		this.log.info( message );
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
	protected void upgradeStarting( UpgradeSegment segment )
	{
		flush();
		switch( segment.getType() )
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
				Assert.fail( "Unknown segment type: " + segment.getType() );
		}
		if( segment.getSource() == null )
			this.buffer.append( " to \"" + segment.getTarget() + "\"" );
		else
			this.buffer.append( " \"" + segment.getSource() + "\" to \"" + segment.getTarget() + "\"" );
		flush();
	}

	@Override
	protected void executing( Command command )
	{
		if( command.isPersistent() )
		{
			for( int i = 0; i < this.messages.length; i++ )
			{
				String m = this.messages[ i ];
				if( m != null )
				{
					flush();
					this.buffer = new StringBuilder().append( SPACES, 0, i * 4 ).append( m ).append( "..." );
					this.messages[ i ] = null;
				}
			}

			flush();
		}
	}

	@Override
	protected void executed()
	{
		// Nothing to do
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
		flush();
	}
}
