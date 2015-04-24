package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import solidbase.http.Response;
import solidbase.http.Servlet;

public class DefaultServlet implements Servlet
{
	public void call( RequestContext context, Parameters params )
	{
		Response response = context.getResponse();

		String url = context.getRequest().getUrl();
		if( url.startsWith( "/" ) )
			url = url.substring( 1 );

		InputStream in = DefaultServlet.class.getClassLoader().getResourceAsStream( url );
		if( in == null )
		{
			response.setStatusCode( 404, url + " not Found" );
			return;
		}

		int pos = url.lastIndexOf( '.' );
		if( pos > 0 )
		{
			if( pos > url.lastIndexOf( '/' ) )
			{
				String extension = url.substring( pos + 1 );
				if( extension.equals( "properties" ) )
					response.setContentType( "text/plain", "ISO-8859-1" );
				else if( extension.equals( "ico" ) )
					response.setContentType( "image/vnd.microsoft.icon", null );
				else if( extension.equals( "js" ) )
					response.setContentType( "text/javascript", null );
				else if( extension.equals( "css" ) )
					response.setContentType( "text/css", null );
			}
		}

		response.setHeader( "Cache-Control", "max-age=3600" );

		try
		{
			try
			{
				OutputStream out = response.getOutputStream();
				byte[] buffer = new byte[ 4096 ];
				int len = in.read( buffer );
				while( len >= 0 )
				{
					out.write( buffer, 0, len );
					len = in.read( buffer );
				}
			}
			finally
			{
				in.close();
			}
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}
}
