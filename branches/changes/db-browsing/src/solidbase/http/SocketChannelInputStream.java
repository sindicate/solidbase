package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import solidbase.util.Assert;


public class SocketChannelInputStream extends InputStream
{
	protected SocketChannelAdapter adapter;
	protected ByteBuffer buffer;

	public SocketChannelInputStream( SocketChannelAdapter adapter )
	{
		this.adapter = adapter;
		this.buffer = ByteBuffer.allocate( 1024 );
		this.buffer.flip();
	}

	@Override
	public int read() throws IOException
	{
		if( !this.buffer.hasRemaining() )
			if( !readChannel() )
				return -1;

		return this.buffer.get();
	}

	@Override
	public int read( byte[] b, int off, int len ) throws IOException
	{
		if( !this.buffer.hasRemaining() )
			if( !readChannel() )
				return -1;

		if( len > this.buffer.remaining() )
			len = this.buffer.remaining();
		this.buffer.get( b, off, len );
		return len;
	}

	@Override
	public int available() throws IOException
	{
		return this.buffer.remaining();
	}

	protected boolean readChannel()
	{
		Assert.isFalse( this.buffer.hasRemaining() );
		Assert.isFalse( this.adapter.isClosed() );

		this.buffer.clear();

		SocketChannel channel = this.adapter.channel;

		try
		{
			int read = channel.read( this.buffer );
			System.out.println( "Channel (" + DebugId.getId( channel ) + ") read #" + read + " bytes from channel" );
			while( read == 0 )
			{
				System.out.println( "Channel (" + DebugId.getId( channel ) + ") Waiting for data" );
				try
				{
					synchronized( this )
					{
						wait();
					}
				}
				catch( InterruptedException e )
				{
					throw new FatalSocketException( e );
				}
				System.out.println( "Channel (" + DebugId.getId( channel ) + ") Waiting for data, ready" );

				read = channel.read( this.buffer );
				System.out.println( "Channel (" + DebugId.getId( channel ) + ") read #" + read + " bytes from channel" );
			}

			if( read == -1 )
				return false;
			this.buffer.flip();
			return true;
		}
		catch( IOException e )
		{
			throw new FatalSocketException( e );
		}
	}
}
