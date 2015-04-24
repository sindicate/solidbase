package solidbase.http;

public class Util
{
	static public void printEscaped( ResponseWriter writer, String text )
	{
		for( char ch : text.toCharArray() )
		{
			if( ch == '&' )
				writer.write( "&amp;" );
			else if( ch == '<' )
				writer.write( "&lt;" );
			else if( ch == '>' )
				writer.write( "&gt;" );
			else
				writer.write( ch );
		}
	}
}
