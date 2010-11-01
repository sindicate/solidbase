package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;

import solidbase.core.SystemException;

public class GZipResponse extends Response
{
	protected Response response;

	public GZipResponse( Response response )
	{
		this.response = response;
		this.out = new GZipResponseOutputStream( response );
	}

	@Override
	public ResponseOutputStream getOutputStream()
	{
		return this.out;
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
		super.reset();
		this.response.reset();
	}

	@Override
	public void flush()
	{
		super.flush();
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

	public void finish()
	{
		try
		{
			( (GZipResponseOutputStream)this.out ).out.finish();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
