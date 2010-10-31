package solidbase.http;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dispatcher
{
	static protected List< ServletMapping > mappings = new ArrayList< ServletMapping >();

	static public void dispatch( Request request, Response response )
	{
		for( ServletMapping mapping : mappings )
		{
			Matcher matcher = mapping.pattern.matcher( request.getUrl() );
			if( matcher.matches() )
			{
				if( mapping.names != null )
				{
					for( int i = 0; i < mapping.names.length; i++ )
					{
						String name = mapping.names[ i ];
						request.addParameter( name, matcher.group( i + 1 ) );
					}
				}
				mapping.servlet.call( request, response );
				return;
			}
		}

		response.setStatusCode( 404, "Not Found" );
	}

	static public void registerServlet( String pattern, Servlet servlet )
	{
		mappings.add( new ServletMapping( Pattern.compile( pattern ), servlet ) );
	}

	static public void registerServlet( String pattern, String names, Servlet servlet )
	{
		mappings.add( new ServletMapping( Pattern.compile( pattern ), names, servlet ) );
	}
}
