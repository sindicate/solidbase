package solidbase.http;

import java.io.OutputStream;

public interface Servlet
{
	void call( Request request, OutputStream response );
}
