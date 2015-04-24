package solidbase.http;

import java.io.IOException;
import java.io.OutputStream;


public class GZipResponse extends Response
{
	protected Response response;

	public GZipResponse( Response response )
	{
		this.out = new GZipResponseOutputStream( response );
		this.response = response;
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
	public void finish()
	{
		super.finish();
		try
		{
			( (GZipResponseOutputStream)this.out ).out.finish();
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
//		this.response.finish();
	}

	@Override
	public void setContentType( String contentType, String charSet )
	{
		super.setContentType( contentType, charSet );
		this.response.setContentType( contentType, charSet );
	}
}
