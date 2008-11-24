package ronnie.dbpatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import com.logicacmg.idt.commons.util.Assert;

public class Console
{
	static protected int col = 0;
	static protected BufferedReader stdin;
	static protected boolean fromAnt;

	static protected void println()
	{
		System.out.println();
		col = 0;
	}

	static protected void print( String string )
	{
		if( col == 0 )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			System.out.print( dateFormat.format( new Date() ) );
			System.out.print( "   " );
		}
		System.out.print( string );
		col += string.length();
	}

	static protected void println( String string )
	{
		if( col == 0 )
		{
			DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
			System.out.print( dateFormat.format( new Date() ) );
			System.out.print( "   " );
		}
		System.out.println( string );
		col = 0;
	}

	static protected void carriageReturn()
	{
		if( col > 0 )
			println();
	}

	static protected void emptyLine()
	{
		carriageReturn();
		println();
	}

	static synchronized protected String input() throws IOException
	{
		if( fromAnt )
			carriageReturn();
		if( stdin == null )
			stdin = new BufferedReader( new InputStreamReader( System.in ) );

		String input = stdin.readLine();
		Assert.notNull( input, "No more input" );

		col = 0;
		return input;
	}
}
