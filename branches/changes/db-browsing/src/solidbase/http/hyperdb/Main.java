package solidbase.http.hyperdb;

import solidbase.http.ApplicationContext;
import solidbase.http.DefaultServlet;
import solidbase.http.Server;
import solidbase.http.TestServlet;

public class Main
{
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		ApplicationContext context = new ApplicationContext();

		context.registerServlet( "/tables/([^/]*)/recordcount", "tablename", new TableRecordCountServlet() );
		context.registerServlet( "/tables/([^/]*)", "tablename", new TableServlet() );
		context.registerServlet( "/tables", new TablesServlet() );
		context.registerServlet( "/test", new TestServlet() );
		context.registerServlet( "", new RootServlet() );
//		context.registerServlet( ".*\\.jsp", new JspServlet() );
		context.registerServlet( ".*", new DefaultServlet() );

//		context.registerFilter( ".*", new CompressionFilter() );

		context.setJspBase( "solidbase.http.hyperdb" );

		try
		{
			new Server().start( context, 80 );
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
