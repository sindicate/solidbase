package solidbase.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

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
			BufferedReader reader = new BufferedReader( new InputStreamReader( in, "ISO_8859-1" ) );
			String line = reader.readLine();
			while( line != null && line.length() > 0 )
			{
				System.out.println( line );
				line = reader.readLine();
			}

//			byte[] buffer = new byte[ 4096 ];
//			int len = in.read( buffer );
//			while( len >= 0 )
//			{
//				System.out.write( buffer );
//				len = in.read( buffer );
//			}

			OutputStream out = socket.getOutputStream();
			PrintWriter writer = new PrintWriter( out );
			writer.println( "HTTP/1.1 200" );
			writer.println();
			writer.println( "Hello World!" );
			writer.flush();
//			out.flush();
//			writer.close();
//			out.close();
//
//			in.close();

			socket.close();
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
