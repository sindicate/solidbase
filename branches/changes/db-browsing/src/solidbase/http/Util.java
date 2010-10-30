package solidbase.http;

import java.io.PrintWriter;

public class Util
{
	static public void printEscaped( PrintWriter writer, String text )
	{
		for( char ch : text.toCharArray() )
		{
			if( ch == '&' )
				writer.print( "&amp;" );
			else if( ch == '<' )
				writer.print( "&lt;" );
			else if( ch == '>' )
				writer.print( "&gt;" );
			else
				writer.print( ch );
		}
	}
}
