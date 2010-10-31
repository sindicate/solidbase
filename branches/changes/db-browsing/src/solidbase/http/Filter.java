package solidbase.http;

public interface Filter
{
	void call( Request request, Response response, FilterChain chain );
}
