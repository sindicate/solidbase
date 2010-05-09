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

import java.io.File;
import java.net.URL;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import solidbase.core.Command;
import solidbase.core.Patch;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;
import solidbase.core.SQLFile;


/**
 * Implements the progress listener for the Apache Ant task.
 * 
 * @author René M. de Bloois
 */
public class Progress extends ProgressListener
{
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
