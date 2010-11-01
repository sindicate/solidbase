package solidbase.http;

import java.io.IOException;

import solidbase.core.SystemException;

public class CompressionFilter implements Filter
{
	public void call( Request request, Response response, FilterChain chain )
	{
		response.setHeader( "Content-Encoding", "gzip" );
		GZipResponse gzipResponse = new GZipResponse( response );
		chain.call( request, gzipResponse );
		try
		{
			gzipResponse.out.out.finish();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
