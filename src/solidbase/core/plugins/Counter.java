package solidbase.core.plugins;

import solidbase.core.ProgressListener;


public class Counter
{
	private solidbase.util.Counter counter;
	private ProgressListener listener;

	public Counter( solidbase.util.Counter counter, ProgressListener progressListener )
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
