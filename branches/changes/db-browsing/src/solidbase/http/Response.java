package solidbase.http;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import solidbase.core.SystemException;

public class Response
{
	protected OutputStream out;
	protected PrintWriter writer;

	public Response( OutputStream out )
	{
		this.out = out;
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
			throw new IllegalStateException( "PrintWriter already created" );
		try
		{
			return this.writer = new PrintWriter( new OutputStreamWriter( this.out, encoding ) );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}
}
