package solidbase.http;

import java.io.PrintWriter;

public class TestServlet extends Servlet
{
	@Override
	public void call( Request request, Response response )
	{
		PrintWriter writer = response.getPrintWriter();
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "Test." );
		writer.flush();
	}
}
