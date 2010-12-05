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
			System.out.println( "* selecting" );
			int selected = selector.select();
			Set< SelectionKey > keys = selector.selectedKeys();
			System.out.println( "* selected:#keys = " + selected + ":" + keys.size() );

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
						System.out.println( "* (" + DebugId.getId( channel ) + ") <-- (new)" );
						channel.configureBlocking( false );
						channel.register( selector, SelectionKey.OP_READ );
					}
					else
						System.out.println( "no channel" );
				}
				else if( key.isReadable() )
				{
					final SocketChannel channel = (SocketChannel)key.channel();
					System.out.println( "* (" + DebugId.getId( channel ) + ") <-- data" );
					System.out.println( "* (" + DebugId.getId( channel ) + ") <-- no read" );
					key.interestOps( 0 );
					SocketChannelAdapter adapter = (SocketChannelAdapter)key.attachment();
					if( adapter != null )
					{
						adapter.readable();
					}
					else
					{
						adapter = new SocketChannelAdapter( channel, key );
						key.attach( adapter );
						System.out.println( "* (" + DebugId.getId( channel ) + ") handler added" );
						Handler handler = new Handler( adapter, context )
						{
							@Override
							public void end() throws IOException
							{
								key.attach( null );
								System.out.println( "* (" + DebugId.getId( channel ) + ") handler removed" );
								if( channel.isOpen() )
								{
									System.out.println( "* (" + DebugId.getId( channel ) + ") <-- want read" );
									key.interestOps( key.interestOps() | SelectionKey.OP_READ ); // TODO This has race conditions
									System.out.println( "* (" + DebugId.getId( channel ) + ") wakeup" );
									selector.wakeup();
								}
								else
									System.out.println( "* (" + DebugId.getId( channel ) + ") <-- closed" );
							}
						};
						handler.start();
					}
				}
				else
					throw new HttpException( "Unexpected ops: " + key.readyOps() );
			}
		}
	}
}
