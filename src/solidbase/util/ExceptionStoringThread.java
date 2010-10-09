package solidbase.util;

/**
 * Thread that stores uncaught exceptions.
 * 
 * @author René M. de Bloois
 */
abstract public class ExceptionStoringThread extends Thread
{
	private RuntimeException exception;
	private boolean threadDeath;

	@Override
	public void run()
	{
		try
		{
			runHandled();
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
	abstract public void runHandled();

	/**
	 * Returns the RuntimeException that the thread has thrown.
	 * 
	 * @return the RuntimeException that the thread has thrown. Null if no exception has been thrown.
	 */
	public RuntimeException getException()
	{
		return this.exception;
	}

	public boolean isThreadDeath()
	{
		return this.threadDeath;
	}
}
