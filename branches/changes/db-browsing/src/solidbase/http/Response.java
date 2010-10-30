package solidbase.http;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.core.SystemException;

public class Response
{
	protected OutputStream out;
	protected PrintWriter writer;
	protected Map< String, List< String > > headers = new HashMap< String, List<String> >();
	protected boolean committed;
	protected int statusCode = 200;
	protected String statusMessage = "OK";

	public Response( OutputStream out )
	{
		this.out = new ResponseOutputStream( this, out );
	}

	public OutputStream getOutputStream()
	{
		return this.out;
	}

	public PrintWriter getPrintWriter()
	{
		return this.writer == null ? this.writer = new PrintWriter( this.out ) : this.writer;
	}

	public PrintWriter getPrintWriter( String encoding )
	{
		if( this.writer != null )
			this.writer.flush();
		try
		{
			return this.writer = new PrintWriter( new OutputStreamWriter( this.out, encoding ) );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}

	public void setHeader( String name, String value )
	{
		List< String > values = new ArrayList< String >();
		values.add( value );
		this.headers.put( name, values );
	}

	public void writeHeader( OutputStream out )
	{
		try
		{
			PrintWriter writer = new PrintWriter( new OutputStreamWriter( out, "ISO-8859-1" ) );
			writer.print( "HTTP/1.1 " );
			writer.print( Integer.toString( this.statusCode ) );
			writer.print( " " );
			writer.println( this.statusMessage );
			for( Map.Entry< String, List< String > > entry : this.headers.entrySet() )
				for( String value : entry.getValue() )
					writer.println( entry.getKey() + ": " + value );
			writer.println();
			writer.flush(); // TODO flush() gets cascaded
			this.committed = true;
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}

	public boolean isCommitted()
	{
		return this.committed;
	}

	public void setStatusCode( int code, String message )
	{
		this.statusCode = code;
		this.statusMessage = message;
	}
}
