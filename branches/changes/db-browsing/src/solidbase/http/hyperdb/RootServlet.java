package solidbase.http.hyperdb;

import solidbase.http.Fragment;
import solidbase.http.RequestContext;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;

public class RootServlet implements Servlet, Fragment
{
	public void call( RequestContext context )
	{
		new Template().call( context, "SolidBrowser", this );
	}

	public void fragment( RequestContext context )
	{
		ResponseWriter writer = context.getResponse().getWriter();
		writer.write( "<a href=\"/tables\">tables</a>\n" );
	}
}
