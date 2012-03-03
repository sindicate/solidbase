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

package solidbase.test.mocks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import mockit.Mock;

import org.testng.Assert;

import solidstack.io.URLRandomAccessLineReader;


/**
 * A mock implementation of {@link URLRandomAccessLineReader}.
 * 
 * @author René M. de Bloois
 */
public class MockRandomAccessLineReader
{
	/**
	 * The contents of the simulated file.
	 */
	protected String contents;

	/**
	 * A reader needed to read lines from the simulated contents.
	 */
	protected BufferedReader reader;

	/**
	 * The current line number.
	 */
	protected int currentLineNumber;

	/**
	 * Get populated by the real class instance.
	 */
	public URLRandomAccessLineReader it;

	/**
	 * Constructs a new mock instance.
	 * 
	 * @param contents The contents of the simulated file.
	 */
	public MockRandomAccessLineReader( String contents )
	{
		this.contents = contents;
		this.reader = new BufferedReader( new StringReader( this.contents ) );
		this.currentLineNumber = 1;
	}

	/**
	 * Mock implementation of {@link URLRandomAccessLineReader#RandomAccessLineReader(URL)}
	 * 
	 * @param url Not used. See {@link URLRandomAccessLineReader#RandomAccessLineReader(URL)}.
	 */
	@Mock
	public void $init( @SuppressWarnings( "unused" ) URL url )
	{
		// Nothing
	}

	/**
	 * Mock implementation of {@link URLRandomAccessLineReader#readLine()}.
	 * 
	 * @return A line from the simulated file.
	 * @throws IOException Whenever an {@link IOException} is thrown by the underlying subsystem.
	 */
	@Mock
	public String readLine() throws IOException
	{
		String result = this.reader.readLine();
		if( result != null )
			this.currentLineNumber++;
		return result;
	}

	/**
	 * Mock implementation of {@link URLRandomAccessLineReader#getLineNumber()}.
	 * 
	 * @return The current line number within the simulated file.
	 */
	@Mock
	public int getLineNumber()
	{
		return this.currentLineNumber;
	}

	/**
	 * Mock implementation of {@link URLRandomAccessLineReader#gotoLine(int)}.
	 * 
	 * @param lineNumber The line number to jump to in the simulated file.
	 * @throws IOException Whenever an {@link IOException} is thrown by the underlying subsystem.
	 */
	@Mock
	public void gotoLine( int lineNumber ) throws IOException
	{
		Assert.assertEquals( lineNumber, 1 );
		this.reader = new BufferedReader( new StringReader( this.contents ) );
		this.currentLineNumber = 1;
	}
}
