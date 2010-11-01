package solidbase.http;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import solidbase.core.SystemException;

public class GZipResponseOutputStream extends ResponseOutputStream
{
	protected ResponseOutputStream rout;
	protected GZIPOutputStream out;

	public GZipResponseOutputStream( Response response, ResponseOutputStream out )
	{
		super( response, out );
		this.rout = out;
		try
		{
			this.out = new GZIPOutputStream( out );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
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
			throw new SystemException( e );
		}
	}

	@Override
	public void clear()
	{
		this.rout.clear();
		try
		{
			this.out = new GZIPOutputStream( this.rout );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
