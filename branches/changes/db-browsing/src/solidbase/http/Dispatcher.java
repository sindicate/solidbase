package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import solidbase.util.Assert;

public class Dispatcher
{
	static protected List< ServletMapping > mappings = new ArrayList< ServletMapping >();

	static public void dispatch( Request request, OutputStream response )
	{
		for( ServletMapping mapping : mappings )
			if( mapping.pattern.equals( request.getUrl() ) )
			{
				mapping.servlet.call( request, response );
				return;
			}

		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 404" );
		writer.println();
		writer.flush();
	}

	static public void registerServlet( String url, Servlet servlet )
	{
		Assert.isFalse( url.endsWith( "/" ) );
		mappings.add( new ServletMapping( url, servlet ) );
	}
}
