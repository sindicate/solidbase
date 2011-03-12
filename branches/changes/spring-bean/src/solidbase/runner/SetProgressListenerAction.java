package solidbase.runner;

import solidbase.core.ProgressListener;

public class SetProgressListenerAction implements Action
{
	protected ProgressListener listener;

	public SetProgressListenerAction( ProgressListener listener )
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
