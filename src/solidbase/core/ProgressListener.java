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

import java.io.File;
import java.net.URL;

/**
 *
 * @author R.M. de Bloois
 * @since Apr 14, 2006
 */
public class ProgressListener
{
	protected void openingPatchFile( File patchFile )
	{
		// could be implemented in subclass
	}

	protected void openingPatchFile( URL patchFile )
	{
		// could be implemented in subclass
	}

	public void openedPatchFile( PatchFile patchFile )
	{
		// could be implemented in subclass
	}

	protected void patchStarting( Patch patch )
	{
		// could be implemented in subclass
	}

	protected void executing( Command command, String message )
	{
		// could be implemented in subclass
	}

	protected void exception( Command command )
	{
		// could be implemented in subclass
	}

	protected void executed()
	{
		// could be implemented in subclass
	}

	protected void patchFinished()
	{
		// could be implemented in subclass
	}

	protected void patchingFinished()
	{
		// could be implemented in subclass
	}

	protected String requestPassword( String user )
	{
		// could be implemented in subclass
		return null;
	}

	protected void skipped( Command command )
	{
		// could be implemented in subclass
	}

	protected void debug( String message )
	{
		// could be implemented in subclass
	}
}
