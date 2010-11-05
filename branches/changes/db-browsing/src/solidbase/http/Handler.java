package solidbase.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import solidbase.core.SystemException;
import solidbase.util.LineReader;
import solidbase.util.PushbackReader;

public class Handler extends Thread
{
	protected Socket socket;

	public Handler( Socket socket )
	{
		this.socket = socket;
	}

	public void handle() throws IOException
	{
		Socket socket = this.socket;

		InputStream in = socket.getInputStream();
		PushbackReader reader = new PushbackReader( new LineReader( new BufferedReader( new InputStreamReader( in, "ISO_8859-1" ) ) ) );

		Request request = new Request();

		RequestTokenizer requestTokenizer = new RequestTokenizer( reader );
		Token token = requestTokenizer.get();
		if( !token.equals( "GET" ) )
			throw new SystemException( "Only GET requests are supported" );

		String url = requestTokenizer.get().getValue();
		token = requestTokenizer.get();
		if( !token.equals( "HTTP/1.1" ) )
			throw new SystemException( "Only HTTP/1.1 requests are supported" );

		System.out.println( "GET " + url + " HTTP/1.1" );

		String parameters = null;
		int pos = url.indexOf( '?' );
		if( pos >= 0 )
		{
			parameters = url.substring( pos + 1 );
			url = url.substring( 0, pos );

			String[] pars = parameters.split( "&" );
			for( String par : pars )
			{
				pos = par.indexOf( '=' );
				if( pos >= 0 )
					request.addParameter( par.substring( 0, pos ), par.substring( pos + 1 ) );
				else
					request.addParameter( par, null );
			}
		}
		if( url.endsWith( "/" ) )
			url = url.substring( 0, url.length() - 1 );
		request.setUrl( url );
		request.setParameters( parameters );

		requestTokenizer.getNewline();

		HttpHeaderTokenizer headerTokenizer = new HttpHeaderTokenizer( reader );
		Token field = headerTokenizer.getField();
		while( !field.isEndOfInput() )
		{
			Token value = headerTokenizer.getValue();
			request.headers.add( new Header( field.getValue(), value.getValue() ) );
			field = headerTokenizer.getField();
		}

//		for( Header f : request.headers )
//			System.out.println( f.field + ": " + f.value );

		Response response = new Response( socket.getOutputStream() );
		RequestContext context = new RequestContext( request, response );
		try
		{
			Dispatcher.dispatch( context );
		}
		catch( Throwable t )
		{
			if( t.getClass().equals( SystemException.class ) && t.getCause() != null )
				t = t.getCause();
			t.printStackTrace( System.err );
			if( !response.isCommitted() )
			{
				response.reset();
				response.setStatusCode( 500, "Exception" );
				response.setHeader( "Content-Type", "text/plain; charset=ISO-8859-1" );
				PrintWriter writer = response.getPrintWriter( "ISO-8859-1" );
				t.printStackTrace( writer );
				writer.flush();
			}
		}

		response.flush();
		socket.close();
	}

	@Override
	public void run()
	{
		try
		{
			handle();
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
