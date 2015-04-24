package solidbase.http;

public interface Filter
{
	void call( RequestContext request, FilterChain chain );
}
