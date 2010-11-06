package solidbase.http.hyperdb;

import solidbase.http.Parameters;
import solidbase.http.RequestContext;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;

public class RootServlet implements Servlet
{
	public void call( RequestContext context, Parameters params )
	{
		new Template().call( context, params.put( "title", null ).put( "body", new Servlet()
		{
			public void call( RequestContext request, Parameters params )
			{
				ResponseWriter writer = request.getResponse().getWriter();
				writer.write( "<a href=\"/tables\">tables</a>\n" );
			}
		}));
	}
}
