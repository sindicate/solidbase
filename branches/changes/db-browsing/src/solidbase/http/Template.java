package solidbase.http;

import java.io.PrintWriter;

public class Template
{
//	protected Map< String, Fragment > fragments = new HashMap< String, Fragment >();

	public void call( Request request, Response response, Fragment fragment )
	{
		PrintWriter writer = response.getPrintWriter();
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "<html>" );
		writer.println( "<head>" );
		writer.println( "	<link rel=\"stylesheet\" type=\"text/css\" href=\"/styles.css\" />" );
		writer.println( "</head>" );
		writer.println( "<body>" );

//		this.fragments.get( "body" ).fragment( request, response );
		fragment.fragment( request, response );

		writer.println( "</body>" );
		writer.println( "</html>" );
		writer.flush();
	}

//	public Template addFragment( String name, Fragment fragment )
//	{
//		this.fragments.put( name, fragment );
//		return this;
//	}
}
