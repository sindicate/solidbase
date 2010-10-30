package solidbase.http;

import java.io.PrintWriter;

import solidbase.util.Assert;

public class RootServlet extends Servlet
{
	@Override
	public void call( Request request, Response response )
	{
		new Template().call( request, response, this );
	}

	@Override
	public void fragment( Request request, Response response, String fragment )
	{
		Assert.isTrue( "body".equals( fragment ) );

		PrintWriter writer = response.getPrintWriter();
		writer.println( "<a href=\"/tables\">tables</a>" );
		writer.flush();
	}
}
