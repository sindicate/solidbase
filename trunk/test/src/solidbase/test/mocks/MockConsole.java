/*--
 * Copyright 2006 René M. de Bloois
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

package solidbase.test.mocks;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.output.TeeOutputStream;

import solidbase.Console;
import solidbase.core.Assert;


/**
 * A stub console used during testing.
 * 
 * @author René M. de Bloois
 */
public class MockConsole extends Console
{
	/**
	 * Responses to give back when input is requested.
	 */
	protected Queue< String > answerQueue = new LinkedList< String >();

	/**
	 * Buffer to collect the output.
	 */
	protected ByteArrayOutputStream outputBuffer;

	/**
	 * Buffer to collect the error output.
	 */
	protected ByteArrayOutputStream errorBuffer;


	/**
	 * Constructor.
	 */
	public MockConsole()
	{
		this.prefixWithDate = false;
		this.outputBuffer = new ByteArrayOutputStream();
		this.errorBuffer = new ByteArrayOutputStream();
		OutputStream originalOut = this.out;
		this.out = new PrintStream( new TeeOutputStream( this.outputBuffer, this.out ) );
		this.err = new PrintStream( new TeeOutputStream( this.errorBuffer, originalOut ) );
	}

	/**
	 * Add a response.
	 * 
	 * @param answer Response to be added to the response queue.
	 */
	public void addAnswer( String answer )
	{
		this.answerQueue.add( answer );
	}

	/**
	 * Returns the output collected.
	 * 
	 * @return The output collected.
	 */
	public String getOutput()
	{
		return this.outputBuffer.toString();
	}

	/**
	 * Returns the error output collected.
	 * 
	 * @return The error output collected.
	 */
	public String getErrorOutput()
	{
		return this.errorBuffer.toString();
	}

	@Override
	public synchronized String input( boolean password )
	{
		String input = this.answerQueue.poll();
		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}
}
