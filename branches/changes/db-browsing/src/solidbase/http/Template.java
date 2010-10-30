package solidbase.http;

import java.io.PrintWriter;

public class Template
{
//	protected Map< String, Fragment > fragments = new HashMap< String, Fragment >();

	public void call( Request request, Response response, String title, Fragment fragment )
	{
		response.setHeader( "Content-Type", "text/html; charset=UTF-8" );

		PrintWriter writer = response.getPrintWriter( "UTF-8" );
		writer.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		writer.println( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" );
		writer.println( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">" );
		writer.println( "<head>" );
		writer.println( "	<title>" + title + "</title>" );
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
