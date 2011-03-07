package solidbase.runner;

import solidbase.core.ProgressListener;

public class SetProgressListener implements Step
{
	protected ProgressListener listener;

	public SetProgressListener( ProgressListener listener )
	{
		if( listener == null )
			throw new NullPointerException( "listener is null" );
		this.listener = listener;
	}

	public void execute( Runner runner )
	{
		runner.listener = this.listener;
	}
}
