package solidbase.http.hyperdb;

import solidbase.http.Fragment;
import solidbase.http.RequestContext;
import solidbase.http.Servlet;

public class TableServlet implements Servlet, Fragment
{
	public void call( RequestContext context )
	{
		String table = context.getRequest().getParameter( "tablename" );
		new Template().call( context, "SolidBrowser - table " + table, this );
	}

	public void fragment( RequestContext context )
	{
		context.getApplication().callJsp( "table_jsp", context );
	}
}
