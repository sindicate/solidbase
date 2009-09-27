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

package ronnie.dbpatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import com.logicacmg.idt.commons.util.Assert;

public class Console
{
	protected int col = 0;
	protected BufferedReader stdin;
	protected boolean fromAnt;
	protected boolean prefixWithDate = true;
	protected PrintStream out = System.out;
	protected PrintStream err = System.err;
	protected java.io.Console java6console;


	public Console()
	{
		try
		{
			this.java6console = System.console();
		}
		catch( NoSuchMethodError e )
		{
			// Only works in java 6
		}
	}

	protected void printBare( String string )
	{
		if( this.java6console != null )
		{
			this.java6console.writer().print( string );
			this.java6console.flush();
		}
		else
			this.out.print( string );
		this.col += string.length();
	}

	protected void printlnBare( String string )
	{
		if( this.java6console != null )
		{
			this.java6console.writer().println( string );
			this.java6console.flush();
		}
		else
			this.out.println( string );
		this.col = 0;
	}

	protected void println()
	{
		printlnBare( "" );
	}

	protected void print( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			printBare( dateFormat.format( new Date() ) + "   " );
		}
		printBare( string );
	}

	protected void println( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			printBare( dateFormat.format( new Date() ) + "   " );
		}
		printlnBare( string );
	}

	protected void carriageReturn()
	{
		if( this.col > 0 )
			printlnBare( "" );
	}

	protected void emptyLine()
	{
		carriageReturn();
		printlnBare( "" );
	}

	protected String input() throws IOException
	{
		return input( false );
	}

	synchronized protected String input( boolean password ) throws IOException
	{
		if( this.fromAnt )
			carriageReturn();

		String input;
		if( this.java6console != null )
		{
			if( password )
				input = new String( this.java6console.readPassword() );
			else
				input = this.java6console.readLine();
		}
		else
		{
			if( this.stdin == null )
				this.stdin = new BufferedReader( new InputStreamReader( System.in ) );
			input = this.stdin.readLine();
		}

		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}

	protected void printStacktrace( Throwable t )
	{
		t.printStackTrace( this.err );
	}
}
