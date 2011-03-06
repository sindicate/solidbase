package solidbase.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solidbase.core.ProgressListener;

public class ProgressLogger extends ProgressListener
{
	static private final Logger logger = LoggerFactory.getLogger( "SOLIDBASE" );

	@Override
	public void cr()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void println( String message )
	{
		logger.info( message );
	}
}
