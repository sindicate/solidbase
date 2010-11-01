package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;

public class GZipResponse extends Response
{
	protected Response response;
	protected GZipResponseOutputStream out;
	protected ResponseWriter writer;

	public GZipResponse( Response response )
	{
		super( response.out );

		this.response = response;
		this.out = new GZipResponseOutputStream( this, super.getOutputStream() );
	}

	@Override
	public ResponseOutputStream getOutputStream()
	{
		return this.out;
	}

	@Override
	public ResponseWriter getWriter()
	{
		if( this.writer != null )
			return this.writer;
		return getWriter( "ISO-8859-1" );
	}

	@Override
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

	@Override
	public PrintWriter getPrintWriter( String encoding )
	{
		return new PrintWriter( getWriter( encoding ) );
	}

	@Override
	public void setHeader( String name, String value )
	{
		this.response.setHeader( name, value );
	}

	@Override
	public void writeHeader( OutputStream out )
	{
		this.response.writeHeader( out );
	}

	@Override
	public boolean isCommitted()
	{
		return this.response.isCommitted();
	}

	@Override
	public void setStatusCode( int code, String message )
	{
		this.response.setStatusCode( code, message );
	}

	@Override
	public void reset()
	{
		this.response.reset();
	}

	@Override
	public void flush()
	{
		this.response.flush();
	}

	@Override
	public void setContentType( String contentType, String charSet )
	{
		this.response.setContentType( contentType, charSet );
	}

	@Override
	public void setContentType( String contentType )
	{
		this.response.setContentType( contentType );
	}
}
