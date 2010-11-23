package solidbase.http;

public class CompressionFilter implements Filter
{
	public void call( RequestContext context, FilterChain chain )
	{
		context.getResponse().setHeader( "Content-Encoding", "gzip" );
		GZipResponse gzipResponse = new GZipResponse( context.getResponse() );
		chain.call( new RequestContext( context.getRequest(), gzipResponse, context.applicationContext ) );
		gzipResponse.finish();
	}
}
