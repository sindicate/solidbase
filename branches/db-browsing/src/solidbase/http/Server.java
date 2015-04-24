package solidbase.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class Server
{
	protected boolean nio = false;

	public void start( ApplicationContext context, int port ) throws IOException
	{
		if( this.nio )
			startThreadPerRequest( context, port );
		else
		{
			ServerSocket server = new ServerSocket( port );
			while( true )
			{
				Socket socket = server.accept();
				Handler handler = new Handler( new SocketAdapter( socket ), context );
				handler.start();
			}
		}
	}

	public void startThreadPerRequest( ApplicationContext context, int port ) throws IOException
	{
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking( false );
		server.socket().bind( new InetSocketAddress( port ) );

		final Selector selector = Selector.open();
		server.register( selector, SelectionKey.OP_ACCEPT );

		while( true )
		{
			System.out.println( "Selector selecting from " + selector.keys().size() + " keys" );
			int selected = selector.select();
			Set< SelectionKey > keys = selector.selectedKeys();
			System.out.println( "Selector selected = " + selected + ":" + keys.size() );

			for( Iterator< SelectionKey > i = keys.iterator(); i.hasNext(); )
			{
				final SelectionKey key = i.next();
				i.remove();

				if( key.isAcceptable() )
				{
					server = (ServerSocketChannel)key.channel();
					SocketChannel channel = server.accept();
					if( channel != null )
					{
						System.out.println( "Channel (" + DebugId.getId( channel ) + ") New channel" );
						channel.configureBlocking( false );
						channel.register( selector, SelectionKey.OP_READ );
					}
					else
						System.out.println( "Selector bogus accept" );
				}
				else if( key.isReadable() )
				{
					final SocketChannel channel = (SocketChannel)key.channel();
					SocketChannelAdapter adapter = (SocketChannelAdapter)key.attachment();
					if( adapter != null )
					{
						System.out.println( "Channel (" + DebugId.getId( channel ) + ") Data ready, unregister" );
						adapter.removeInterest( SelectionKey.OP_READ );
						adapter.readable();
					}
					else
					{
						final SocketChannelAdapter adapter2 = new SocketChannelAdapter( channel, key );
						System.out.println( "Channel (" + DebugId.getId( channel ) + ") Data ready, unregister" );
						adapter2.removeInterest( SelectionKey.OP_READ );
						if( !adapter2.inputStream.readChannel() )
						{
							// Appearantly the channel is closing
							System.out.println( "Channel (" + DebugId.getId( channel ) + ") Close" );
							channel.close();
						}
						else
						{
							System.out.println( "Channel (" + DebugId.getId( channel ) + ") Handler attach/start" );
							key.attach( adapter2 );
							Handler handler = new Handler( adapter2, context )
							{
								@Override
								public void end() throws IOException
								{
									System.out.println( "Channel (" + DebugId.getId( channel ) + ") Handler detach/end" );
									key.attach( null );
									if( channel.isOpen() )
									{
										System.out.println( "Channel (" + DebugId.getId( channel ) + ") Register" );
										adapter2.addInterest( SelectionKey.OP_READ );
									}
									else
										System.out.println( "Channel (" + DebugId.getId( channel ) + ") Closed" );
								}
							};
							handler.start();
						}
					}
				}
				else if( key.isWritable() )
				{
					final SocketChannel channel = (SocketChannel)key.channel();
					System.out.println( "Channel (" + DebugId.getId( channel ) + ") Write ready, unregister" );
					SocketChannelAdapter adapter = (SocketChannelAdapter)key.attachment();
					if( adapter != null )
					{
						adapter.removeInterest( SelectionKey.OP_WRITE );
						adapter.writeable();
					}
				}
//				else if( key.isConnectable() )
//				{
//					final SocketChannel channel = (SocketChannel)key.channel();
//					System.out.println( "Channel (" + DebugId.getId( channel ) + ") Connection event" );
//				}
				else
					throw new HttpException( "Unexpected ops: " + key.readyOps() );
			}
		}
	}
}
