package solidbase.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solidbase.core.ProgressListener;

/**
 * An implementation of {@link ProgressListener} that logs all messages to the 'solidbase' logger.
 *
 * @author René M. de Bloois
 */
public class ProgressLogger extends ProgressListener
{
	static private final Logger logger = LoggerFactory.getLogger( "solidbase" );

	@Override
	public void cr()
	{
		//
	}

	@Override
	public void println( String message )
	{
		logger.info( message );
	}
}
