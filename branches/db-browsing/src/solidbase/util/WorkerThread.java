package solidbase.util;

/**
 * Worker thread which stores the exception in case it ended with one. It also stores if the thread ended itself by throwing a ThreadDeath error.
 * 
 * @author René M. de Bloois
 */
abstract public class WorkerThread extends Thread
{
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
			this.exception = e;
		}
		catch( ThreadDeath e )
		{
			this.threadDeath = true;
		}
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
