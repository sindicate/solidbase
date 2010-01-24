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

import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision

import java.util.jar.JarFile


// Scan all jar files
def jars = ant.fileScanner
{
	fileset( dir: "." ) 
	{
		include( name: "**/*.jar" )
	}
}

// Get svn client
SVNRepositoryFactoryImpl.setup()
def svn = SVNClientManager.newInstance( null, "rbloois", "rbloois" ).updateClient

def modules = [:]
//modules[ "postplaza-waf" ] = [ url: "", rev: 1 ] // for test

jars.each
{	
	jarname ->
	def jarfile = new JarFile( jarname )
	def manifest = jarfile.getJarEntry( "META-INF/MANIFEST.MF" )
	if( manifest != null ) 
	{	
		def svnUrl = null
		def svnRev = null
		def projectName = null

		// Read MANIFEST.MF and scan for svn-url, svn-revision and project-name		
		jarfile.getInputStream( manifest ).eachLine 
		{	
			if( it.toLowerCase().startsWith( "svn-url: " ) )
				svnUrl = it[ 9..-1 ]
			else if( it.toLowerCase().startsWith( "svn-revision: " ) )
				svnRev = it[ 14..-1 ]
			else if( it.toLowerCase().startsWith( "project-name: " ) )
				projectName = it[ 14..-1 ]
		}
		
		if( svnUrl != null ) 
		{
			assert svnRev != null : "svn-revision not found in MANIFEST.MF"
			assert projectName != null : "project-name not found in MANIFEST.MF"
			
			svnRev = new Long( svnRev )
		
			// If same module already encountered, choose the one with the larger revision number
			def exists = modules[ projectName ]
			if( exists != null )
				if( exists.rev < svnRev )
				{
					ant.echo( "Warning: duplicate module ${projectName} found but with unequal revisions: ${exists.rev} and ${svnRev}" )
					exists = null
				}
				else if( exists.rev > svnRev )
					ant.echo( "Warning: duplicate module ${projectName} found but with unequal revisions: ${exists.rev} and ${svnRev}" )
			
			if( exists == null )
				modules[ projectName ] = [ url: svnUrl, rev: svnRev ]
		}
	}
}

// Loop through the modules and export the sources
modules.each
{
	module, attributes ->
	
	ant.echo( "Getting ${module}'s source from ${attributes.url}, revision ${attributes.rev}" )
	def rev = SVNRevision.create( attributes.rev )
	svn.doExport( 
			SVNURL.parseURIDecoded( attributes.url ), 
			"${properties.'build.home'}/modules/${module}" as File, 
			rev, rev, null, false, true 
	)
}
