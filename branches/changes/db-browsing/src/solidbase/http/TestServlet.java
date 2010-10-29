package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;

public class TestServlet implements Servlet
{
	public void call( Request request, OutputStream response )
	{
		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "Test." );
		writer.flush();
	}
}
