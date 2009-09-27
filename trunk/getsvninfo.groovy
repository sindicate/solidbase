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

task.taskName = "svninfo"

def modifiedcount = 0

def clientManager = SVNClientManager.newInstance()

def info = clientManager.WCClient.doInfo( "." as File, null )

properties."svn.committedDate" = info.committedDate
properties."svn.repositoryRootURL" = info.repositoryRootURL
properties."svn.url" = info.URL
properties."svn.revision" = info.revision
properties."svn.committedRevision" = info.committedRevision

println( "URL: ${info.URL}, rev: ${info.revision}" )
