package solidbase.http;

import java.io.PrintWriter;
import java.util.List;

public class TablesServlet implements Servlet, Fragment
{
	public void call( Request request, Response response )
	{
		new Template().call( request, response, "SolidBrowser - tables", this );
	}

	public void fragment( Request request, Response response )
	{
		PrintWriter writer = response.getPrintWriter();

		List< Table > tables = Database.getTables();

		writer.println( "<table>" );
		writer.println( "<tr><th>Table</th><th># records</th></tr>" );
		for( Table table : tables )
		{
			writer.print( "<tr><td><a href=\"/table:" );
			writer.print( table.name );
			writer.print( "\">" );
			writer.print( table.name );
			writer.print( "</a></td><td>" );
			writer.print( Integer.toString( table.records ) );
			writer.println( "</td></tr>" );
		}
		writer.println( "</table>" );
	}
}
