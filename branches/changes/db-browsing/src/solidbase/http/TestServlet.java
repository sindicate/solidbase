package solidbase.http;

public class TestServlet implements Servlet
{
	public void call( RequestContext context )
	{
		ResponseWriter writer = context.getResponse().getWriter();
		writer.write( "HTTP/1.1 200 OK\n" );
		writer.write( '\n' );
		writer.write( "Test.\n" );
	}
}
