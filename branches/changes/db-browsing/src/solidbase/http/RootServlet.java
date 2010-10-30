package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;

public class RootServlet implements Servlet
{
	public void call( Request request, OutputStream response )
	{
		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "<html><body><a href=\"/tables\">tables</a></body></html>" );
		writer.flush();
	}
}
