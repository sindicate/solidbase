package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SocketChannelAdapter implements SocketProxy
{
	protected SocketChannel channel;
	protected ByteBuffer buffer;
	protected SocketChannelInputStream inputStream;
	protected SocketChannelOutputStream outputStream;

	public SocketChannelAdapter( SocketChannel channel, SelectionKey key )
	{
		this.channel = channel;
		this.inputStream = new SocketChannelInputStream( channel, key );
		this.outputStream = new SocketChannelOutputStream( channel );
	}

	public void readable()
	{
		synchronized( this.inputStream )
		{
			this.inputStream.notify();
		}
	}

	public InputStream getInputStream()
	{
		return this.inputStream;
	}

	public OutputStream getOutputStream()
	{
		return this.outputStream;
	}

	public void close() throws IOException
	{
		this.channel.close();
	}

	public boolean isClosed()
	{
		return !this.channel.isOpen();
	}

	public boolean isThreadPerConnection()
	{
		return false;
	}
}
