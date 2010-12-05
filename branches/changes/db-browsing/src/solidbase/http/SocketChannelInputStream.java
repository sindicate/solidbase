package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class SocketChannelInputStream extends InputStream
{
	protected SocketChannel channel;
	protected SelectionKey key;
	protected ByteBuffer buffer;

//	protected boolean end;

	public SocketChannelInputStream( SocketChannel channel, SelectionKey key )
	{
		this.channel = channel;
		this.key = key;
		this.buffer = ByteBuffer.allocate( 1024 );
		this.buffer.flip();
	}

	@Override
	public int read() throws IOException
	{
//		if( end )
//			return -1;
		if( this.buffer.hasRemaining() )
			return this.buffer.get();
		this.buffer.clear();
		int read = this.channel.read( this.buffer );
		while( read == 0 )
		{
			try
			{
				synchronized( this )
				{
					System.out.println( "* (" + DebugId.getId( this.channel ) + ") <-- want read" );
					this.key.interestOps( this.key.interestOps() | SelectionKey.OP_READ );
					this.wait();
				}
			}
			catch( InterruptedException e )
			{
				throw new IOException( e );
			}
			read = this.channel.read( this.buffer );
		}
		if( read == -1 )
			return -1;
		this.buffer.flip();
		return this.buffer.get();
	}

	@Override
	public int read( byte[] b, int off, int len ) throws IOException
	{
//		if( b == null )
//			throw new NullPointerException();
//		if( off < 0 || len < 0 || len > b.length - off )
//			throw new IndexOutOfBoundsException();
//		if( len == 0 )
//			return 0;

		if( !this.buffer.hasRemaining() )
		{
			this.buffer.clear();
			int read = this.channel.read( this.buffer );
			while( read == 0 )
			{
				try
				{
					synchronized( this )
					{
						this.wait();
					}
				}
				catch( InterruptedException e )
				{
					throw new IOException( e );
				}
				read = this.channel.read( this.buffer );
			}
			if( read == -1 )
				return -1;
			this.buffer.flip();
		}

		if( len > this.buffer.remaining() )
			len = this.buffer.remaining();
		this.buffer.get( b, off, len );
		return len;
	}
}
