package solidbase.http;

public class ServletMapping
{
	protected String pattern;
	protected Servlet servlet;

	public ServletMapping( String pattern, Servlet servlet )
	{
		this.pattern = pattern;
		this.servlet = servlet;
	}
}
