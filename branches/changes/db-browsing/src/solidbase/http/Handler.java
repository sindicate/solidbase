package solidbase.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import solidbase.util.LineReader;
import solidbase.util.PushbackReader;


/**
 * Thread that handles an incoming connection.
 * 
 * @author René M. de Bloois
 */
public class Handler extends Thread
{
	private Socket socket;
	private ApplicationContext applicationContext;
//	static int counter;

	/**
	 * Constructor.
	 * 
	 * @param socket The incoming connection.
	 * @param applicationContext The {@link ApplicationContext}.
	 */
	public Handler( Socket socket, ApplicationContext applicationContext )
	{
		this.socket = socket;
		this.applicationContext = applicationContext;
	}

	/**
	 * This method actually handles the connection.
	 * 
	 * @throws IOException Whenever the socket throws an {@link IOException}.
	 */
	public void handle() throws IOException
	{
		try
		{
			System.out.println( "SOCKET: Incoming connection" );

			Socket socket = this.socket;

			while( !socket.isClosed() )
			{
				InputStream in = socket.getInputStream();
				PushbackReader reader = new PushbackReader( new LineReader( new BufferedReader( new InputStreamReader( in, "ISO-8859-1" ) ) ) );

				Request request = new Request();

				RequestTokenizer requestTokenizer = new RequestTokenizer( reader );
				Token token = requestTokenizer.get();
				if( !token.equals( "GET" ) )
					throw new HttpException( "Only GET requests are supported" );

				String url = requestTokenizer.get().getValue();
				token = requestTokenizer.get();
				if( !token.equals( "HTTP/1.1" ) )
					throw new HttpException( "Only HTTP/1.1 requests are supported" );

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
//					System.out.println( "    "+ field.getValue() + " = " + value.getValue() );
					request.addHeader( field.getValue(), value.getValue() );
					field = headerTokenizer.getField();
				}

//				String filename = "response" + (++counter) + ".out";
//				OutputStream file = new FileOutputStream( filename );
//				try
//				{
				OutputStream out = socket.getOutputStream();
//					out = new TeeOutputStream( out, file );
				out = new CloseBlockingOutputStream( out );
				Response response = new Response( request, out );
				RequestContext context = new RequestContext( request, response, this.applicationContext );
				try
				{
					this.applicationContext.dispatch( context );
				}
				catch( Throwable t )
				{
					if( t.getClass().equals( HttpException.class ) && t.getCause() != null )
						t = t.getCause();
					t.printStackTrace( System.err );
					if( !response.isCommitted() )
					{
						response.reset();
						response.setStatusCode( 500, "Internal Server Error" );
						response.setContentType( "text/plain", "ISO-8859-1" );
						PrintWriter writer = response.getPrintWriter( "ISO-8859-1" );
						t.printStackTrace( writer );
						writer.flush();
					}
				}

				response.finish();

				// TODO Detect Connection: close headers on the request & response
				// TODO A GET request has no body, when a POST comes without content size, the connection should be closed.
				// TODO What about socket.getKeepAlive() and the other properties?

				String length = response.getHeader( "Content-Length" );
				if( length == null )
				{
					String transfer = response.getHeader( "Transfer-Encoding" );
					if( !"chunked".equals( transfer ) )
						socket.close();
				}

				if( !socket.isClosed() )
				{
					String connection = request.getHeader( "Connection" );
					if( request.isConnectionClose() )
						socket.close();
				}
//				}
//				finally
//				{
//					file.close();
//				}
			}
		}
		finally
		{
			this.socket.close();
			System.out.println( "SOCKET: Connection closed" );
		}
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
