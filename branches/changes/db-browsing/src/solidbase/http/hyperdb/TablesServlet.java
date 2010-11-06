package solidbase.http.hyperdb;

import solidbase.http.RequestContext;
import solidbase.http.Servlet;


public class TablesServlet implements Servlet
{
	public void call( RequestContext context )
	{
		context.callJsp( "tables_jsp" );
	}
}
