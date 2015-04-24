import org.tmatesoft.svn.core.wc.*

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
	throw new Exception( "You have uncommitted changes in your working copy, please commit or revert" )

// Check that the revisions are all equal

def revision = null

clientManager.WCClient.doInfo( "." as File, null, true,
	{ 
		info ->
		if( revision == null )
			revision = info.revision
		else if( info.revision != revision )
			throw new Exception( "The items in the working copy have unequal revision numbers, please update" )
	} as ISVNInfoHandler
)
