package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketAdapter implements SocketProxy
{
	protected Socket channel;

	public SocketAdapter( Socket socket )
	{
		this.channel = socket;
	}

	public InputStream getInputStream() throws IOException
	{
		return this.channel.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException
	{
		return this.channel.getOutputStream();
	}

	public void close() throws IOException
	{
		this.channel.close();
	}

	public boolean isClosed()
	{
		return this.channel.isClosed();
	}

	public boolean isThreadPerConnection()
	{
		return true;
	}
}
