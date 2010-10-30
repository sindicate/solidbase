package solidbase.http;

import java.io.PrintWriter;

public class Template
{
	public void call( Request request, Response response, Servlet servlet )
	{
		PrintWriter writer = response.getPrintWriter();
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "<html>" );
		writer.println( "<head>" );
		writer.println( "	<link rel=\"stylesheet\" type=\"text/css\" href=\"/styles.css\" />" );
		writer.println( "</head>" );
		writer.println( "<body>" );

		servlet.fragment( request, response, "body" );

		writer.println( "</body>" );
		writer.println( "</html>" );
		writer.flush();
	}
}
