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
	protected SelectionKey key;
	protected ByteBuffer buffer;
	protected SocketChannelInputStream inputStream;
	protected SocketChannelOutputStream outputStream;

	public SocketChannelAdapter( SocketChannel channel, SelectionKey key )
	{
		this.channel = channel;
		this.key = key;
		this.inputStream = new SocketChannelInputStream( this );
		this.outputStream = new SocketChannelOutputStream( this );
	}

	public void readable()
	{
		synchronized( this.inputStream )
		{
			System.out.println( "Channel (" + DebugId.getId( this.channel ) + ") Data ready, notify" );
			this.inputStream.notify();
		}
	}

	public void writeable()
	{
		synchronized( this.outputStream )
		{
			System.out.println( "Channel (" + DebugId.getId( this.channel ) + ") Write ready, notify" );
			this.outputStream.notify();
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

	public void addInterest( int interest )
	{
		synchronized( this.key )
		{
			this.key.interestOps( this.key.interestOps() | interest );
		}
		this.key.selector().wakeup();
	}

	public void removeInterest( int interest )
	{
		synchronized( this.key )
		{
			this.key.interestOps( this.key.interestOps() ^ interest );
		}
		this.key.selector().wakeup();
	}
}
