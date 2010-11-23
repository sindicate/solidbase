package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.util.Assert;

public class Response
{
//	static protected int count = 1;

	protected Request request;
	protected ResponseOutputStream out;
	protected ResponseWriter writer;
	protected PrintWriter printWriter;
	protected Map< String, List< String > > headers = new HashMap< String, List<String> >();
	protected boolean committed;
	protected int statusCode = 200;
	protected String statusMessage = "OK";
	protected String contentType;
	protected String charSet;

	protected Response()
	{
	}

	public Response( Request request, OutputStream out )
	{
		this.request = request;
		this.out = new ResponseOutputStream( this, out );
	}

	public ResponseOutputStream getOutputStream()
	{
		return this.out;
	}

	public ResponseWriter getWriter()
	{
		if( this.writer != null )
			return this.writer;
		if( this.charSet != null )
			return getWriter( this.charSet );
		return getWriter( "ISO-8859-1" );
	}

	public ResponseWriter getWriter( String encoding )
	{
		if( this.writer != null )
		{
			if( this.writer.getEncoding().equals( encoding ) )
				return this.writer;
			this.writer.flush();
		}
		return this.writer = new ResponseWriter( this.out, encoding );
	}

	public PrintWriter getPrintWriter( String encoding )
	{
		return new PrintWriter( getWriter( encoding ) );
	}

	public void setHeader( String name, String value )
	{
		if( this.committed )
			throw new IllegalStateException( "Response is already committed" );
		if( name.equals( "Content-Type" ) )
			throw new IllegalArgumentException( "Content type should be set with setContentType()" );
		setHeader0( name, value );
	}

	protected void setHeader0( String name, String value )
	{
		List< String > values = new ArrayList< String >();
		values.add( value );
		this.headers.put( name, values );
	}

	public void writeHeader( OutputStream out )
	{
		if( this.request.isConnectionClose() )
			setHeader( "Connection", "close" );
		else
			if( getHeader( "Content-Length" ) == null ) // TODO What about empty string?
				setHeader0( "Transfer-Encoding", "chunked" );

		if( this.contentType != null )
			if( this.charSet != null )
				setHeader0( "Content-Type", this.contentType + "; charset=" + this.charSet );
			else
				setHeader0( "Content-Type", this.contentType );

//		System.out.println( "Response:" );
		ResponseWriter writer = new ResponseWriter( new FlushBlockingOutputStream( out ), "ISO-8859-1" );
		writer.write( "HTTP/1.1 " );
		writer.write( Integer.toString( this.statusCode ) );
		writer.write( " " );
		writer.write( this.statusMessage );
		writer.write( '\r' );
		writer.write( '\n' );
		for( Map.Entry< String, List< String > > entry : this.headers.entrySet() )
			for( String value : entry.getValue() )
			{
				writer.write( entry.getKey() );
				writer.write( ": " );
				writer.write( value );
				writer.write( '\r' );
				writer.write( '\n' );
//				System.out.println( "    " + entry.getKey() + " = " + value );
			}
		writer.write( '\r' );
		writer.write( '\n' );
		writer.flush();
		this.committed = true;

		// TODO Are these header names case sensitive or not? And the values like 'chunked'?
		if( "chunked".equals( getHeader( "Transfer-Encoding" ) ) )
			this.out.out = new ChunkedOutputStream( this.out.out );
	}

	public boolean isCommitted()
	{
		return this.committed;
	}

	public void setStatusCode( int code, String message )
	{
		if( this.committed )
			throw new IllegalStateException( "Response is already committed" );
		this.statusCode = code;
		this.statusMessage = message;
	}

	public void reset()
	{
		if( this.committed )
			throw new IllegalStateException( "Response is already committed" );
		getOutputStream().clear();
		this.writer = null;
		this.statusCode = 200;
		this.statusMessage = "OK";
		this.headers.clear();
	}

	public void flush()
	{
		if( this.writer != null )
			this.writer.flush();
		getOutputStream().flush();
	}

	public void finish()
	{
		flush();
		getOutputStream().close();
	}

	public void setContentType( String contentType, String charSet )
	{
		if( this.committed )
			throw new IllegalStateException( "Response is already committed" );
		this.contentType = contentType;
		this.charSet = charSet;
	}

	public String getHeader( String name )
	{
		List< String > values = this.headers.get( name );
		if( values == null )
			return null;
		Assert.isTrue( !values.isEmpty() );
		if( values.size() > 1 )
			throw new IllegalStateException( "Found more than 1 value for the header " + name );
		return values.get( 0 );
	}
}
