package solidbase.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

		RequestTokenizer requestTokenizer = new RequestTokenizer( reader );
		Token token = requestTokenizer.get();
		if( !token.equals( "GET" ) )
			throw new SystemException( "Only GET requests are supported" );
		Token url = requestTokenizer.get();
		token = requestTokenizer.get();
		if( !token.equals( "HTTP/1.1" ) )
			throw new SystemException( "Only HTTP/1.1 requests are supported" );
		requestTokenizer.getNewline();

		System.out.println( "GET " + url + " HTTP/1.1" );

		if( !url.equals( "/" ) )
		{
			OutputStream out = socket.getOutputStream();
			PrintWriter writer = new PrintWriter( out );
			writer.println( "HTTP/1.1 404" );
			writer.println();
			writer.flush();
		}
		else
		{
			HttpHeaderTokenizer headerTokenizer = new HttpHeaderTokenizer( reader );
			RequestHeader header = new RequestHeader();
			Token field = headerTokenizer.getField();
			while( !field.isEndOfInput() )
			{
				Token value = headerTokenizer.getValue();
				header.addField( field.getValue(), value.getValue() );
				field = headerTokenizer.getField();
			}

			for( HeaderField f : header.fields )
				System.out.println( f.field + ": " + f.value );

			OutputStream out = socket.getOutputStream();
			PrintWriter writer = new PrintWriter( out );
			writer.println( "HTTP/1.1 200" );
			writer.println();
			writer.println( "Hello World!" );
			writer.flush();
		}

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
