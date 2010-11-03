// Called by build.xml

import solidbase.http.JspTranslater

def fileset = ant.fileset( dir: args[ 0 ], includes: "**/*.jsp" )
def outputdir = new File( args[ 1 ] )

def scanner = fileset.directoryScanner

solidbase.http.JspTranslater.translatePages( scanner.basedir, scanner.includedFiles, outputdir )
