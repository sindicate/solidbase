package solidbase.util;

/**
 * Worker thread which stores the exception in case it ended with one. It also stores if the thread ended itself by throwing a ThreadDeath error.
 * 
 * @author René M. de Bloois
 */
abstract public class WorkerThread extends Thread
{
	private StackTraceElement[] startTrace;
	private RuntimeException exception;
	private boolean threadDeath;

	/**
	 * Constructor.
	 */
	public WorkerThread()
	{
		super( "Worker" );
	}

	@Override
	public void run()
	{
		try
		{
			work();
		}
		catch( RuntimeException e )
		{
			StackTraceElement[] exceptionTrace = e.getStackTrace();
			StackTraceElement[] newTrace = new StackTraceElement[ exceptionTrace.length + this.startTrace.length ];
			System.arraycopy( exceptionTrace, 0, newTrace, 0, exceptionTrace.length );
			System.arraycopy( this.startTrace, 0, newTrace, exceptionTrace.length, this.startTrace.length );
			e.setStackTrace( newTrace );
			this.exception = e;
		}
		catch( ThreadDeath e )
		{
			this.threadDeath = true;
		}
	}

	@Override
	public synchronized void start()
	{
		this.startTrace = Thread.currentThread().getStackTrace();
		if( this.startTrace.length > 0 && this.startTrace[ 0 ].getMethodName().equals( "getStackTrace" ) )
		{
			StackTraceElement[] newTrace = new StackTraceElement[ this.startTrace.length - 1 ];
			System.arraycopy( this.startTrace, 1, newTrace, 0, newTrace.length );
			this.startTrace = newTrace;
		}
		super.start();
	}

	/**
	 * Override to implement the thread. If a RuntimeException is thrown, it is stored, and can be retrieved by {@link #getException()}.
	 */
	abstract public void work();

	/**
	 * Returns the RuntimeException that the thread has thrown.
	 * 
	 * @return the RuntimeException that the thread has thrown. Null if no exception has been thrown.
	 */
	public RuntimeException getException()
	{
		return this.exception;
	}

	/**
	 * Returns true if the thread threw a {@link ThreadDeath} error to indicated that it was interrupted, false otherwise.
	 * 
	 * @return true if the thread threw a {@link ThreadDeath} error to indicated that it was interrupted, false otherwise.
	 */
	public boolean isThreadDeath()
	{
		return this.threadDeath;
	}
}
