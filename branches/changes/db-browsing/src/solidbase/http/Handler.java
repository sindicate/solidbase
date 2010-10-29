package solidbase.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import solidbase.http.HttpHeaderTokenizer.Token;
import solidbase.util.LineReader;

public class Handler extends Thread
{
	protected Socket socket;

	public Handler( Socket socket )
	{
		this.socket = socket;
	}

	@Override
	public void run()
	{
		Socket socket = this.socket;
		try
		{
			InputStream in = socket.getInputStream();
			LineReader reader = new LineReader( new BufferedReader( new InputStreamReader( in, "ISO_8859-1" ) ) );
			String request = reader.readLine();

			System.out.println( request );

			HttpHeaderTokenizer httpHeaderTokenizer = new HttpHeaderTokenizer( reader );
			RequestHeader header = new RequestHeader();
			Token field = httpHeaderTokenizer.getField();
			while( !field.isEndOfInput() )
			{
				Token value = httpHeaderTokenizer.getValue();
				header.addField( field.getValue(), value.getValue() );
				field = httpHeaderTokenizer.getField();
			}

			for( HeaderField f : header.fields )
				System.out.println( f.field + ": " + f.value );

			OutputStream out = socket.getOutputStream();
			PrintWriter writer = new PrintWriter( out );
			writer.println( "HTTP/1.1 200" );
			writer.println();
			writer.println( "Hello World!" );
			writer.flush();

			socket.close();
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
