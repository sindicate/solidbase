package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Dispatcher
{
	static protected Map< String, Servlet > mappings = new HashMap< String, Servlet >();

	static public void dispatch( Request request, OutputStream response )
	{
		for( Map.Entry< String, Servlet > mapping : mappings.entrySet() )
			if( mapping.getKey().equals( request.getUrl() ) )
			{
				mapping.getValue().call( request, response );
				return;
			}

		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 404" );
		writer.println();
		writer.flush();
	}

	static public void registerServlet( String url, Servlet servlet )
	{
		mappings.put( url, servlet );
	}
}
