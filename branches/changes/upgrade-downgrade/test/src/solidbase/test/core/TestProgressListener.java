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

package solidbase.test.core;

import java.io.File;
import java.net.URL;

import solidbase.core.Command;
import solidbase.core.ProgressListener;

public class TestProgressListener extends ProgressListener
{

	@Override
	protected void debug( String message )
	{
		System.out.println( "DEBUG: " + message );
	}

	@Override
	protected void exception( Command command )
	{
		System.out.println( "EXCEPTION: " + command );
	}

	@Override
	protected void executed()
	{
		System.out.println( "EXECUTED." );
	}

	@Override
	protected void executing( Command command, String message )
	{
		System.out.println( "EXECUTING: " + message );
	}

	@Override
	protected void openingPatchFile( File patchFile )
	{
		System.out.println( "OPENINGPATCHFILE: " + patchFile );
	}

	@Override
	protected void openingPatchFile( URL patchFile )
	{
		System.out.println( "OPENINGPATCHFILE: " + patchFile );
	}

	@Override
	protected void patchFinished()
	{
		System.out.println( "PATCHFINISHED." );
	}

	@Override
	protected void patchStarting( String source, String target )
	{
		System.out.println( "PATCHSTARTING: " + source + " - " + target );
	}

	@Override
	protected String requestPassword( String user )
	{
		System.out.println( "REQUESTPASSWORD: " + user );
		return null;
	}

	@Override
	protected void skipped( Command command )
	{
		System.out.println( "SKIPPED." );
	}
}
