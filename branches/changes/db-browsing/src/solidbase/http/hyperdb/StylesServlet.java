package solidbase.http.hyperdb;

import java.io.PrintWriter;

import solidbase.http.Request;
import solidbase.http.Response;
import solidbase.http.Servlet;

public class StylesServlet implements Servlet
{
	public void call( Request request, Response response )
	{
		response.setContentType( "text/css", "ISO-8859-1" );
		response.setHeader( "Cache-Control", "max-age=3600" );

		PrintWriter writer = response.getPrintWriter( "ISO-8859-1" );

		writer.println( "body, input, select, textarea, button, pre { font-family: Arial, Helvetica, sans-serif; font-size: 12px; }" );

		writer.println( "body { margin: 8px 8px 8px 8px; background-color: #88F; color: black; }" );
//		writer.println( "form { margin-top: 0px; margin-bottom: 0px; padding: 0px; }" );
		writer.println( "p { margin: 0px 0px 10px 0px; }" );
//		writer.println( "h1 { font-size: 12px; font-weight: bold; margin: 5px 0px; }" );
//		writer.println( "h2 { font-size: 12px; font-weight: bold; margin: 5px 0px; color: #CC0000; }" );
//		writer.println( "h3 { font-size: 10px; font-weight: bold; margin: 5px 0px 0px 0px; }" );
		writer.println( "table { border-collapse: collapse; padding: 0px; border: 0px; border-spacing: 0px; background-color: white; font-size: 12px; }" );
		writer.println( "th, td { padding: 4px; border: 1px solid black; vertical-align: top; }" );
		writer.println( "td.null { background-color: #CCC; }" );
//		writer.println( "img { border: 0px; }" );
//		writer.println( "input, select, textarea { border: 1px solid #999999; padding: 2px; background-color: #FFFFFF; }" );
//		writer.println( "select { padding: 0px; }" );
//		writer.println( "button { background-color: #FFCC00; }" );

		writer.println( "a { color: black; }" );
		writer.println( "a:hover { color: #CC0000; }" );

//		writer.println( "input.button { font-weight: bold; padding: 1px 10px; background-color: #FFCC00; color: #000000; border-top: 1px solid #FFFFFF; border-right: 1px solid #666666; border-bottom: 1px solid #666666; border-left: 1px solid #FFFFFF; }" );
//		writer.println( "button.button { font-weight: bold; padding: 1px 10px; color: #FFCC00; }" );
//		writer.println( "input.checkbox { padding: 0px; color: #000000; border: 0px; background-color: #E4E4E4; }" );
//		writer.println( "input.radio { padding: 0px; color: #000000; border: 0px; background-color: #E4E4E4; }" );
//		writer.println( "input.image { padding: 0px; color: #000000; border: 0px; background-color: #E4E4E4; }" );

		writer.flush();
	}
}
