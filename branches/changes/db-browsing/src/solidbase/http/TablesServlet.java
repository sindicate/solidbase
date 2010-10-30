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

		writer.append( "<table>" );
		writer.append( "<tr><th>Table</th><th># records</th></tr>" );
		for( Table table : tables )
		{
			writer.append( "<tr><td><a href=\"/table:" );
			writer.append( table.name );
			writer.append( "\">" );
			writer.append( table.name );
			writer.append( "</a></td><td>" );
			writer.append( Integer.toString( table.records ) );
			writer.append( "</td></tr>" );
		}
		writer.append( "</table>" );
	}
}
