package solidbase.http.hyperdb;

import solidbase.http.Parameters;
import solidbase.http.RequestContext;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;

public class Template implements Servlet
{
	public void call( RequestContext request, Parameters params )
	{
		String title = (String)params.get( "title" );
		Servlet body = (Servlet)params.get( "body" );

		request.getResponse().setContentType( "text/html", "UTF-8" );

		ResponseWriter writer = request.getResponse().getWriter();
		writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
		writer.write( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" );
		writer.write( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n" );
		writer.write( "<head>\n" );
		writer.write( "	<title>HyperDB" + ( title == null ? "" : " - " + title ) + "</title>\n" );
		writer.write( "	<link rel=\"stylesheet\" type=\"text/css\" href=\"/styles.css\" />\n" );
		writer.write( "</head>\n" );
		writer.write( "<body>\n" );

		body.call( request, params );

		writer.write( "</body>\n" );
		writer.write( "</html>\n" );
		writer.flush();
	}
}
