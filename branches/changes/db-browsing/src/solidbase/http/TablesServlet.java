package solidbase.http;

import java.io.PrintWriter;
import java.util.List;

import solidbase.util.Assert;

public class TablesServlet extends Servlet
{
	@Override
	public void call( Request request, Response response )
	{
		new Template().call( request, response, this );
	}

	@Override
	public void fragment( Request request, Response response, String fragment )
	{
		Assert.isTrue( "body".equals( fragment ) );

		PrintWriter writer = response.getPrintWriter();

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
	}
}
