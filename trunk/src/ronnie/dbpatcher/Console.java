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

	protected void println()
	{
		this.out.println();
		this.col = 0;
	}

	protected void print( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			this.out.print( dateFormat.format( new Date() ) );
			this.out.print( "   " );
		}
		this.out.print( string );
		this.col += string.length();
	}

	protected void println( String string )
	{
		if( this.col == 0 && this.prefixWithDate )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			this.out.print( dateFormat.format( new Date() ) );
			this.out.print( "   " );
		}
		this.out.println( string );
		this.col = 0;
	}

	protected void carriageReturn()
	{
		if( this.col > 0 )
			println();
	}

	protected void emptyLine()
	{
		carriageReturn();
		println();
	}

	synchronized protected String input() throws IOException
	{
		if( this.fromAnt )
			carriageReturn();
		if( this.stdin == null )
			this.stdin = new BufferedReader( new InputStreamReader( System.in ) );

		String input = this.stdin.readLine();
		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}
}
