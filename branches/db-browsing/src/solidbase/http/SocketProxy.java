package solidbase.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SocketProxy
{
	InputStream getInputStream() throws IOException;
	OutputStream getOutputStream() throws IOException;
	void close() throws IOException;
	boolean isClosed();
	boolean isThreadPerConnection();
}
