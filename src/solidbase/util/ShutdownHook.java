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

/**
 * This shutdown hook prevents ctrl-c from aborting all threads.
 * Instead it will call {@link Thread#interrupt()} on the given thread.
 * 
 * @author René M. de Bloois
 */
// TODO Move to the util package
public class ShutdownHook extends Thread
{
	/**
	 * The main thread.
	 */
	protected Thread thread;

	/**
	 * Constructor.
	 * 
	 * @param thread The main thread.
	 */
	public ShutdownHook( Thread thread )
	{
		this.thread = thread;
	}

	/**
	 * This method gets triggered when the JVM wants to exit.
	 * 
	 * @see Runtime#addShutdownHook(Thread)
	 */
	@Override
	public void run()
	{
		try
		{
			this.thread.interrupt();
			this.thread.join();
		}
		catch( InterruptedException e )
		{
			// OK to stop here
		}
	}
}
