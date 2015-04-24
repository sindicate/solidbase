package solidbase.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationContext
{
	protected List< ServletMapping > mappings = new ArrayList< ServletMapping >();
	protected List< FilterMapping > filterMappings = new ArrayList< FilterMapping >();
	protected Map< String, Class< Servlet > > jspCache = new HashMap< String, Class< Servlet > >();
	protected String jspBase;

	public void registerServlet( String pattern, Servlet servlet )
	{
		this.mappings.add( new ServletMapping( Pattern.compile( pattern ), servlet ) );
	}

	public void registerServlet( String pattern, String names, Servlet servlet )
	{
		this.mappings.add( new ServletMapping( Pattern.compile( pattern ), names, servlet ) );
	}

	public void registerFilter( String pattern, Filter filter )
	{
		this.filterMappings.add( new FilterMapping( Pattern.compile( pattern ), filter ) );
	}

	public void setJspBase( String jspBase )
	{
		this.jspBase = jspBase;
	}

	public void dispatch( RequestContext context )
	{
		FilterChain chain = null;

		for( FilterMapping mapping : this.filterMappings )
		{
			Matcher matcher = mapping.pattern.matcher( context.getRequest().getUrl() );
			if( matcher.matches() )
			{
				if( chain == null )
					chain = new FilterChain();
				chain.add( mapping.filter );
			}
		}

		for( ServletMapping mapping : this.mappings )
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
					mapping.servlet.call( context, null );
				return;
			}
		}

		context.getResponse().setStatusCode( 404, "Not Found" );
	}

//	public void callJsp( String name, RequestContext context )
//	{
//		if( this.jspBase != null )
//			name = this.jspBase + "." + name;
//
//		Class< Servlet > jsp = this.jspCache.get( name );
//		if( jsp == null )
//		{
//			try
//			{
//				jsp = ( Class< Servlet > )ApplicationContext.class.getClassLoader().loadClass( name );
//			}
//			catch( ClassNotFoundException e )
//			{
//				throw new HttpException( e );
//			}
//			this.jspCache.put( name, jsp );
//		}
//		Servlet servlet;
//		try
//		{
//			servlet = jsp.newInstance();
//		}
//		catch( InstantiationException e )
//		{
//			throw new HttpException( e );
//		}
//		catch( IllegalAccessException e )
//		{
//			throw new HttpException( e );
//		}
//		servlet.call( context );
//	}
}
