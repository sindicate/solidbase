package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;

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
}
