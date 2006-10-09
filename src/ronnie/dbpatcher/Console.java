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
	static protected DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
	static protected BufferedReader stdin;
	
	static protected void println()
	{
		System.out.println();
		col = 0;
	}
	
	static protected void print( String string )
	{
//		Assert.check( string.indexOf( '\n' ) < 0, "Newlines not allowed" );
		if( col == 0 )
		{
			System.out.print( dateFormat.format( new Date() ) );
			System.out.print( "   " );
		}
		System.out.print( string );
		col += string.length();
	}
	
	static protected void println( String string )
	{
//		Assert.check( string.indexOf( '\n' ) < 0, "Newlines not allowed" );
		if( col == 0 )
		{
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
	
	static protected String input() throws IOException
	{
		if( stdin == null )
			stdin = new BufferedReader( new InputStreamReader( System.in ) );
		
		String input = stdin.readLine();
		Assert.notNull( input, "No more input" );
		
		col = 0;
		return input;
	}
}
