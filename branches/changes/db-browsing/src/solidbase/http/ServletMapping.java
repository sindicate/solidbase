package solidbase.http;

import java.util.regex.Pattern;

public class ServletMapping
{
	protected Pattern pattern;
	protected String[] names;
	protected Servlet servlet;

	public ServletMapping( Pattern pattern, Servlet servlet )
	{
		this.pattern = pattern;
		this.servlet = servlet;
	}

	public ServletMapping( Pattern pattern, String names, Servlet servlet )
	{
		this.pattern = pattern;
		this.servlet = servlet;
		this.names = names.split( "\\s+" );
	}
}
