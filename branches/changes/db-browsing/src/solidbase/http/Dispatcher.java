package solidbase.http;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dispatcher
{
	static protected List< ServletMapping > mappings = new ArrayList< ServletMapping >();
	static protected List< FilterMapping > filterMappings = new ArrayList< FilterMapping >();

	static public void dispatch( RequestContext context )
	{
		FilterChain chain = null;

		for( FilterMapping mapping : filterMappings )
		{
			Matcher matcher = mapping.pattern.matcher( context.getRequest().getUrl() );
			if( matcher.matches() )
			{
				if( chain == null )
					chain = new FilterChain();
				chain.add( mapping.filter );
			}
		}

		for( ServletMapping mapping : mappings )
		{
			Matcher matcher = mapping.pattern.matcher( context.getRequest().getUrl() );
			if( matcher.matches() )
			{
				if( mapping.names != null )
				{
					for( int i = 0; i < mapping.names.length; i++ )
					{
						String name = mapping.names[ i ];
						context.getRequest().addParameter( name, matcher.group( i + 1 ) );
					}
				}
				if( chain != null )
				{
					chain.set( mapping.servlet  );
					chain.call( context );
				}
				else
					mapping.servlet.call( context );
				return;
			}
		}

		context.getResponse().setStatusCode( 404, "Not Found" );
	}

	static public void registerServlet( String pattern, Servlet servlet )
	{
		mappings.add( new ServletMapping( Pattern.compile( pattern ), servlet ) );
	}

	static public void registerServlet( String pattern, String names, Servlet servlet )
	{
		mappings.add( new ServletMapping( Pattern.compile( pattern ), names, servlet ) );
	}

	static public void registerFilter( String pattern, Filter filter )
	{
		filterMappings.add( new FilterMapping( Pattern.compile( pattern ), filter ) );
	}
}
