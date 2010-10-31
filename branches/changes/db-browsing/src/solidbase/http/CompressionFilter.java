package solidbase.http;

public class CompressionFilter implements Filter
{
	public void call( Request request, Response response, FilterChain chain )
	{
		chain.call( request, response );
	}
}
