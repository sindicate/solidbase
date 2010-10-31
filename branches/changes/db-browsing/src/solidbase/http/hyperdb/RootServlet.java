package solidbase.http.hyperdb;

import java.io.Writer;

import solidbase.http.Fragment;
import solidbase.http.Request;
import solidbase.http.Response;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;

public class RootServlet implements Servlet, Fragment
{
	public void call( Request request, Response response )
	{
		new Template().call( request, response, "SolidBrowser", this );
	}

	public void fragment( Request request, Response response )
	{
		ResponseWriter writer = response.getWriter();
		writer.write( "<a href=\"/tables\">tables</a>\n" );
	}
}
