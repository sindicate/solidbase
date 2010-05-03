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

import org.tmatesoft.svn.core.wc.*
import org.apache.tools.ant.BuildException


def modifiedcount = 0

def clientManager = SVNClientManager.newInstance()

// Check if there are modified files in the working copy

clientManager.statusClient.doStatus( "." as File, true, false, false, false, false,
	{ 
		status ->
		println( "${status.file} -> ${status.contentsStatus}" )
		modifiedcount++
	} as ISVNStatusHandler
)

if( modifiedcount > 0 )
	throw new BuildException( "You have uncommitted changes in your working copy, please commit or revert" )

// Check that the revisions are all equal

def revision = null

clientManager.WCClient.doInfo( "." as File, null, true,
	{ 
		info ->
		if( revision == null )
			revision = info.revision
		else if( info.revision != revision )
			throw new BuildException( "The items in the working copy have unequal revision numbers, please update" )
	} as ISVNInfoHandler
)
