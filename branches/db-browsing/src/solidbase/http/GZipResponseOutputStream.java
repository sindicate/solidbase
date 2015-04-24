package solidbase.http;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZipResponseOutputStream extends ResponseOutputStream
{
	protected Response response;
	protected GZIPOutputStream out;

	public GZipResponseOutputStream( Response response )
	{
		this.response = response;
		try
		{
			this.out = new GZIPOutputStream( response.getOutputStream() );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( byte[] b, int off, int len )
	{
		try
		{
			this.out.write( b, off, len );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( byte[] b )
	{
		try
		{
			this.out.write( b );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void write( int b )
	{
		try
		{
			this.out.write( b );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void close()
	{
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void flush()
	{
		try
		{
			this.out.flush();
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}

	@Override
	public void clear()
	{
		this.response.getOutputStream().clear();
		try
		{
			this.out = new GZIPOutputStream( this.response.getOutputStream() );
		}
		catch( IOException e )
		{
			throw new HttpException( e );
		}
	}
}
