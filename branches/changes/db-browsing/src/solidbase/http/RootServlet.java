package solidbase.http;

import java.io.PrintWriter;

public class RootServlet implements Servlet, Fragment
{
	public void call( Request request, Response response )
	{
		new Template().call( request, response, this );
	}

	public void fragment( Request request, Response response )
	{
		PrintWriter writer = response.getPrintWriter();
		writer.println( "<a href=\"/tables\">tables</a>" );
		writer.flush();
	}
}
