package solidbase.http.hyperdb;

import java.util.List;

import solidbase.http.Fragment;
import solidbase.http.Request;
import solidbase.http.Response;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;


public class TablesServlet implements Servlet
{
	public void call( Request request, Response response )
	{
		new Template().call( request, response, "SolidBrowser - tables", new Fragment()
		{
			public void fragment( Request request, Response response )
			{
				ResponseWriter writer = response.getWriter();

				List< Table > tables = Database.getTables();

				writer.write( "<table>\n" );
				writer.write( "<tr><th>Table</th><th># records</th></tr>\n" );
				for( Table table : tables )
				{
					writer.write( "<tr><td><a href=\"/table:" );
					writer.write( table.name );
					writer.write( "\">" );
					writer.write( table.name );
					writer.write( "</a></td><td>" );
					writer.write( Integer.toString( table.records ) );
					writer.write( "</td></tr>\n" );
				}
				writer.write( "</table>\n" );
			}
		} );
	}
}
