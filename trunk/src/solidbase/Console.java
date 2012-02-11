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

package solidbase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import solidbase.core.SystemException;
import solidbase.util.Assert;


/**
 * Represents the console. In java 6, the {@link System#console()} is used. In older versions of java,
 * {@link System#out}, {@link System#in} and {@link System#err} are used. Used by the command line version of SolidBase.
 *
 * @author René M. de Bloois
 */
// TODO Test this on Java 5
public class Console
{
	/**
	 * The current column of the cursor.
	 */
	protected int col = 0;

	/**
	 * The input reader.
	 */
	protected BufferedReader stdin;

	/**
	 * We are running the command line version from within Apache Ant.
	 */
	protected boolean fromAnt;

	/**
	 * Prefix each line with the date and time?
	 */
	protected boolean prefixWithDate = true;

	/**
	 * The output stream.
	 */
	protected PrintStream out = System.out;

	/**
	 * The error output stream.
	 */
	protected PrintStream err = System.err;

	/**
	 * The java 6 {@link System#console()}.
	 */
	protected java.io.Console java6console;


	/**
	 * Constructor.
	 */
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

	/**
	 * Prints the string without prefixing the current date/time.
	 *
	 * @param string The string to print.
	 */
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

	/**
	 * Prints the string without prefixing the current date/time, add a newline to the end.
	 *
	 * @param string The string to print.
	 */
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

	/**
	 * Prints a newline.
	 */
	protected void println()
	{
		printlnBare( "" );
	}

	/**
	 * Prints a string prefixed with the current date/time. Prefixing only happens if {@link #prefixWithDate} is true,
	 * and the current column {@link #col} == 0.
	 *
	 * @param string The string to print.
	 */
	protected void print( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			printBare( dateFormat.format( new Date() ) + "   " );
		}
		printBare( string );
	}

	/**
	 * Prints a string prefixed with the current date/time and with a newline added to the end. Prefixing only happens
	 * if {@link #prefixWithDate} is true, and the current column {@link #col} == 0.
	 *
	 * @param string The string to print.
	 */
	protected void println( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			printBare( dateFormat.format( new Date() ) + "   " );
		}
		printlnBare( string );
	}

	/**
	 * Prints a newline, but only if {@link #col} > 0.
	 */
	protected void carriageReturn()
	{
		if( this.col > 0 )
			printlnBare( "" );
	}

	/**
	 * Makes sure that an empty line is generated after the current printed text.
	 */
	protected void emptyLine()
	{
		carriageReturn();
		printlnBare( "" );
	}

	/**
	 * Input a string.
	 *
	 * @return The string that is input.
	 */
	protected String input()
	{
		return input( false );
	}

	/**
	 * Input a string. If password is true, only * are shown.
	 *
	 * @param password Input a password?
	 * @return The string that is input.
	 */
	synchronized protected String input( boolean password )
	{
		if( this.fromAnt )
			carriageReturn();

		String input;
		if( this.java6console != null )
		{
			if( password )
				input = String.valueOf( this.java6console.readPassword() );
			else
				input = this.java6console.readLine();
		}
		else
		{
			if( this.stdin == null )
				this.stdin = new BufferedReader( new InputStreamReader( System.in ) );
			try
			{
				input = this.stdin.readLine();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}

		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}

	/**
	 * Prints a stacktrace to the error output stream.
	 *
	 * @param t The throwable of which the stacktrace needs to be printed.
	 */
	protected void printStacktrace( Throwable t )
	{
		t.printStackTrace( this.err );
	}
}
