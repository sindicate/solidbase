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
