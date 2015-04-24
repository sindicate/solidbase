package solidbase.http;

public class CompressionFilter implements Filter
{
	public void call( RequestContext context, FilterChain chain )
	{
		context.getResponse().setHeader( "Content-Encoding", "gzip" );
		GZipResponse gzipResponse = new GZipResponse( context.getResponse() );
		try
		{
			chain.call( new RequestContext( context.getRequest(), gzipResponse, context.applicationContext ) );
			gzipResponse.finish();
		}
		catch( FatalSocketException e )
		{
			throw e;
		}
		catch( RuntimeException e )
		{
			gzipResponse.finish();
			throw e;
		}
		catch( Error e )
		{
			gzipResponse.finish();
			throw e;
		}
	}
}
