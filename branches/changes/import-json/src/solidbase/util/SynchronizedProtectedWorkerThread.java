/*--
 * Copyright 2010 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.util;

import solidbase.core.FatalException;
import solidstack.lang.ThreadInterrupted;


/**
 * Worker thread which stores the exception in case it ended with one. It also stores if the thread ended itself by throwing a ThreadDeath error.
 *
 * @author René M. de Bloois
 */
abstract public class SynchronizedProtectedWorkerThread extends Thread
{
	private StackTraceElement[] startTrace;
	private RuntimeException exception;

	/**
	 * Constructor.
	 *
	 * @param name Name of the thread.
	 */
	public SynchronizedProtectedWorkerThread( String name )
	{
		super( name );
	}

	@Override
	public void run()
	{
		ShutdownHook hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		try
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
			catch( ThreadInterrupted i )
			{
				this.exception = new FatalException( "Aborted" ); // TODO Can't use the core FatalException in the util package
			}
		}
		finally
		{
			try
			{
				Runtime.getRuntime().removeShutdownHook( hook );
			}
			catch( IllegalStateException e )
			{
				// Happens when a shutdown is in progress
			}
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
		try
		{
			join();
			if( this.exception != null )
				throw this.exception;
		}
		catch( InterruptedException e )
		{
			Thread.currentThread().interrupt();
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
	 * A thread that captures the shutdown and safely shuts down the main thread.
	 *
	 * @author René de Bloois
	 */
	protected class ShutdownHook extends Thread
	{
		@Override
		public void run()
		{
			System.err.println( "Shutdown in progress, please wait..." );
			try
			{
				SynchronizedProtectedWorkerThread.this.interrupt();
				SynchronizedProtectedWorkerThread.this.join();
			}
			catch( InterruptedException e )
			{
				// OK to stop here
			}
		}
	}
}
