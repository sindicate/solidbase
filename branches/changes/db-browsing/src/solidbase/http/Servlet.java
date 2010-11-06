package solidbase.http;

public interface Servlet
{
	void call( RequestContext request, Parameters params );
}
