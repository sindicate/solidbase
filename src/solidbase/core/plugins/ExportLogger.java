package solidbase.core.plugins;

import solidbase.core.ProgressListener;
import solidbase.util.LogCounter;


public class ExportLogger
{
	private LogCounter counter;
	private ProgressListener listener;

	public ExportLogger( LogCounter counter, ProgressListener progressListener )
	{
		this.counter = counter;
		this.listener = progressListener;
	}

	public void count()
	{
		if( this.counter.next() )
			this.listener.println( "Exported " + this.counter.total() + " records." );
	}

	public void end()
	{
		if( this.counter.needFinal() )
			this.listener.println( "Exported " + this.counter.total() + " records." );
	}
}
