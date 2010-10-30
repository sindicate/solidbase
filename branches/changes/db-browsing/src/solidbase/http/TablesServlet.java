package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public class TablesServlet implements Servlet
{
	public void call( Request request, OutputStream response )
	{
		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "<html>" );
		writer.println( "<body>" );

		List< Table > tables = Database.getTables();

		writer.append( "<table>" );
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

		writer.println( "</body>" );
		writer.println( "</html>" );
		writer.flush();
	}
}
