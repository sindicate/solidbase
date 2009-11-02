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

package solidbase.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import solidbase.config.ConfigListener;
import solidbase.core.Assert;
import solidbase.core.Command;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;


public class Progress extends ProgressListener implements ConfigListener
{
	protected Project project;
	protected Task task;
	protected StringBuilder buffer;

	protected void flush()
	{
		if( this.buffer != null && this.buffer.length() > 0 )
		{
			this.project.log( this.task, this.buffer.toString(), Project.MSG_INFO );
			this.buffer = null;
		}
	}

	protected void info( String message )
	{
		flush();
		this.project.log( this.task, message, Project.MSG_INFO );
	}

	protected void verbose( String message )
	{
		flush();
		this.project.log( this.task, message, Project.MSG_VERBOSE );
	}

	public Progress( Project project, Task task )
	{
		this.project = project;
		this.task = task;
	}

	public void readingPropertyFile( String path )
	{
		verbose( "Reading property file " + path );
	}

	@Override
	protected void openingPatchFile( String patchFile )
	{
		info( "Opening patchfile '" + patchFile + "'" );
	}

	@Override
	public void openedPatchFile( PatchFile patchFile )
	{
		info( "    Encoding is '" + patchFile.getEncoding() + "'" );
	}

	@Override
	protected void patchStarting( String source, String target )
	{
		if( source == null )
			info( "Patching to \"" + target + "\"" );
		else
			info( "Patching \"" + source + "\" to \"" + target + "\"" );
	}

	@Override
	protected void executing( Command command, String message )
	{
		Assert.notNull( message );
		flush();
		this.buffer = new StringBuilder( message );
	}

	@Override
	protected void exception( Command command )
	{
		// The sql is printed by the SQLExecutionException.printStackTrace().
	}

	@Override
	protected void executed()
	{
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
		info( "The database has been patched." );
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
