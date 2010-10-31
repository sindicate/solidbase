package solidbase.http;

public class TestServlet implements Servlet
{
	public void call( Request request, Response response )
	{
		ResponseWriter writer = response.getWriter();
		writer.write( "HTTP/1.1 200 OK\n" );
		writer.write( '\n' );
		writer.write( "Test.\n" );
	}
}
