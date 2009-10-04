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

package ronnie.dbpatcher.test.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.output.TeeOutputStream;

import ronnie.dbpatcher.Console;
import ronnie.dbpatcher.core.Assert;


public class MockConsole extends Console
{
	protected Queue< String > answerQueue = new LinkedList< String >();
	protected ByteArrayOutputStream outputBuffer;
	protected ByteArrayOutputStream errorBuffer;

	public MockConsole()
	{
		this.prefixWithDate = false;
		this.outputBuffer = new ByteArrayOutputStream();
		this.errorBuffer = new ByteArrayOutputStream();
		OutputStream originalOut = this.out;
		this.out = new PrintStream( new TeeOutputStream( this.outputBuffer, this.out ) );
		this.err = new PrintStream( new TeeOutputStream( this.errorBuffer, originalOut ) );
	}

	public void addAnswer( String answer )
	{
		this.answerQueue.offer( answer );
	}

	public String getOutput()
	{
		return this.outputBuffer.toString();
	}

	public String getErrorOutput()
	{
		return this.errorBuffer.toString();
	}

	@Override
	public synchronized String input( boolean password ) throws IOException
	{
		String input = this.answerQueue.poll();
		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}
}
