/*--
 * Copyright 2009 René M. de Bloois
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

package solidbase.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import solidbase.core.Command;
import solidbase.core.UpgradeSegment;
import solidbase.core.ProgressListener;
import solidbase.util.Assert;


/**
 * Implements the progress listener for the Apache Ant task.
 * 
 * @author René M. de Bloois
 */
public class Progress extends ProgressListener
{
	static private final String SPACES = "                                        ";

	/**
	 * The Ant project.
	 */
	protected Project project;

	/**
	 * The Ant task.
	 */
	protected Task task;

	/**
	 * Buffer to collect output before logging.
	 */
	protected StringBuilder buffer;

	/**
	 * Constructor.
	 * 
	 * @param project The Ant project.
	 * @param task The Ant task.
	 */
	public Progress( Project project, Task task )
	{
		this.project = project;
		this.task = task;
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
		this.project.log( this.task, message, Project.MSG_INFO );
	}

	/**
	 * Flush collected output to the project's log.
	 */
	protected void flush()
	{
		if( this.buffer != null && this.buffer.length() > 0 )
		{
			this.project.log( this.task, this.buffer.toString(), Project.MSG_INFO );
			this.buffer = null;
		}
	}

	/**
	 * Log an info message to the project's log.
	 * 
	 * @param message The message to log.
	 */
	protected void info( String message )
	{
		flush();
		this.project.log( this.task, message, Project.MSG_INFO );
	}

	/**
	 * Log a verbose message to the project's log.
	 * 
	 * @param message The message to log.
	 */
	protected void verbose( String message )
	{
		flush();
		this.project.log( this.task, message, Project.MSG_VERBOSE );
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
	protected void executing( Command command, String message )
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

		if( message != null ) // Message can be null, when a message has not been set, but sql is still being executed
		{
			flush();
			this.buffer = new StringBuilder( message ).append( "..." );
		}

		flush();
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
