package ronnie.dbpatcher;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import com.cmg.pas.util.Assert;

public class Console
{
	static protected int col = 0;
	static DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
	
	public static void println()
	{
		System.out.println();
		col = 0;
	}
	
	public static void print( String string )
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
	
	public static void println( String string )
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

	public static void carriageReturn()
	{
		if( col > 0 )
			println();
	}

	public static void emptyLine()
	{
		carriageReturn();
		println();
	}
	
	public static String input() throws IOException
	{
		byte[] buffer = new byte[ 100 ];
		int read = System.in.read( buffer );
		Assert.check( read < 100, "Input too long" );
		Assert.check( buffer[ --read ] == '\n' );
		if( buffer[ read - 1 ] == '\r' )
			read--;

		String input = new String( buffer, 0, read );
		Assert.check( input.length() > 0, "Input too short" );
		
		col = 0;
		return input;
	}
}
