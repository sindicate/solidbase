package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import solidbase.util.Assert;


// TODO Improve performance?
public class SocketChannelOutputStream extends OutputStream
{
	protected SocketChannelAdapter adapter;
	protected ByteBuffer buffer;

	public SocketChannelOutputStream( SocketChannelAdapter adapter )
	{
		this.adapter = adapter;
		this.buffer = ByteBuffer.allocate( 8192 );
	}

	@Override
	public void write( int b )
	{
		Assert.isTrue( this.buffer.hasRemaining() );
		this.buffer.put( (byte)b );
		if( !this.buffer.hasRemaining() )
			writeChannel();
	}

	@Override
	public void write( byte[] b, int off, int len )
	{
		while( len > 0 )
		{
			int l = len;
			if( l > this.buffer.remaining() )
				l = this.buffer.remaining();
			this.buffer.put( b, off, l );
			off += l;
			len -= l;
			if( !this.buffer.hasRemaining() )
				writeChannel();
		}
	}

	@Override
	public void flush() throws IOException
	{
		if( this.buffer.position() > 0 )
			writeChannel();
	}

	protected void writeChannel()
	{
		SocketChannel channel = this.adapter.channel;

		Assert.isTrue( channel.isOpen() && channel.isConnected() );
		this.buffer.flip();
		Assert.isTrue( this.buffer.hasRemaining() );

		try
		{
			int written = channel.write( this.buffer );
//			System.out.println( "Channel (" + DebugId.getId( this.channel ) + ") written #" + written + " bytes to channel" );
			while( this.buffer.hasRemaining() )
			{
				System.out.println( "Channel (" + DebugId.getId( channel ) + ") Waiting for write" );
				this.adapter.addInterest( SelectionKey.OP_WRITE );
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
				System.out.println( "Channel (" + DebugId.getId( channel ) + ") Waiting for write, ready" );
				written = channel.write( this.buffer );
//				System.out.println( "Channel (" + DebugId.getId( this.channel ) + ") written #" + written + " bytes to channel" );
			}

			this.buffer.clear();
		}
		catch( IOException e )
		{
			throw new FatalSocketException( e );
		}
	}
}
