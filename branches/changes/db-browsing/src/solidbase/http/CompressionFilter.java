package solidbase.http;

public class CompressionFilter implements Filter
{
	public void call( Request request, Response response, FilterChain chain )
	{
		response.setHeader( "Content-Encoding", "gzip" );
		GZipResponse gzipResponse = new GZipResponse( response );
		chain.call( request, gzipResponse );
		gzipResponse.finish();
	}
}
