package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


// TODO Improve performance?
public class SocketChannelOutputStream extends OutputStream
{
	protected SocketChannel channel;

	public SocketChannelOutputStream( SocketChannel channel )
	{
		this.channel = channel;
	}

	@Override
	public void write( int b )
	{
//		throw new UnsupportedOperationException();
		ByteBuffer buffer = ByteBuffer.wrap( new byte[] { (byte)b } );
		try
		{
			int written = this.channel.write( buffer );
			if( written == 0 )
				throw new FatalSocketException( "A byte is not written" );
		}
		catch( IOException e )
		{
			throw new FatalSocketException( e );
		}
	}

	@Override
	public void write( byte[] b, int off, int len )
	{
		if( len == 0 )
			return;
		ByteBuffer buffer = ByteBuffer.wrap( b, off, len );
		try
		{
			int written = this.channel.write( buffer );
			if( written != len )
				throw new FatalSocketException( "Not all bytes have been written" );
		}
		catch( IOException e )
		{
			throw new FatalSocketException( e );
		}
	}
}
