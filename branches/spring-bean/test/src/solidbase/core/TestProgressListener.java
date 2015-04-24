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

package solidbase.core;

import solidbase.core.Command;
import solidbase.core.Patch;
import solidbase.core.PatchFile;
import solidbase.core.ProgressListener;
import solidbase.core.SQLExecutionException;
import solidbase.core.SQLFile;
import solidbase.util.Assert;
import solidbase.util.Resource;

public class TestProgressListener extends ProgressListener
{
	@Override
	public void cr()
	{
		Assert.fail( "Should not be called" );
	}

	@Override
	public void println( String message )
	{
		Assert.fail( "Should not be called" );
	}

	@Override
	protected void debug( String message )
	{
		System.out.println( "DEBUG: " + message );
	}

	@Override
	public void print( String message )
	{
		System.out.println( message );
	}

	@Override
	protected void exception( SQLExecutionException exception )
	{
		System.out.println( "EXCEPTION: " + exception );
	}

	@Override
	protected void openingPatchFile( Resource patchFile )
	{
		System.out.println( "OPENINGPATCHFILE: " + patchFile );
	}

	@Override
	protected void openingSQLFile( Resource sqlFile )
	{
		System.out.println( "OPENINGSQLFILE: " + sqlFile );
	}

	@Override
	protected void openedPatchFile( PatchFile patchFile )
	{
		System.out.println( "OPENEDPATCHFILE." );
	}

	@Override
	protected void openedSQLFile( SQLFile sqlFile )
	{
		System.out.println( "OPENEDSQLFILE." );
	}

	@Override
	protected void startSection( int level, String message )
	{
		System.out.println( "STARTSECTION: " + message + ", level " + level );
	}

	@Override
	protected void executing( Command command, String message )
	{
		System.out.println( "EXECUTING: " + message );
	}

	@Override
	protected void executed()
	{
		System.out.println( "EXECUTED." );
	}

	@Override
	protected void patchFinished()
	{
		System.out.println( "PATCHFINISHED." );
	}

	@Override
	protected void patchStarting( Patch patch )
	{
		System.out.println( "PATCHSTARTING: " + patch.getSource() + " - " + patch.getTarget() );
	}

	@Override
	protected void upgradeComplete()
	{
		System.out.println( "PATCHINGFINISHED." );
	}

	@Override
	protected void noUpgradeNeeded()
	{
		System.out.println( "NOUPGRADENEEDED." );
	}

	@Override
	protected void sqlExecutionComplete()
	{
		System.out.println( "SQLEXECUTIONFINISHED." );
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
