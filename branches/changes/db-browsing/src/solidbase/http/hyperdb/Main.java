package solidbase.http.hyperdb;

import java.net.ServerSocket;
import java.net.Socket;

import solidbase.http.ApplicationContext;
import solidbase.http.CompressionFilter;
import solidbase.http.DefaultServlet;
import solidbase.http.Handler;
import solidbase.http.JspServlet;
import solidbase.http.TestServlet;

public class Main
{
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		ApplicationContext context = new ApplicationContext();

		context.registerServlet( "/table:([^/]*)", "tablename", new TableServlet() );
		context.registerServlet( "/tables", new TablesServlet() );
		context.registerServlet( "/test", new TestServlet() );
		context.registerServlet( "/styles.css", new StylesServlet() );
		context.registerServlet( "", new RootServlet() );
		context.registerServlet( ".*\\.jsp", new JspServlet() );
		context.registerServlet( ".*", new DefaultServlet() );

		context.registerFilter( ".*", new CompressionFilter() );

		context.setJspBase( "solidbase.http.hyperdb" );

		try
		{
			ServerSocket server = new ServerSocket( 80 );
			while( true )
			{
				Socket socket = server.accept();
				Handler handler = new Handler( socket, context );
				handler.start();
			}
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
