package solidbase.util;

public class JSONEOFException extends RuntimeException
{
	@Override
	public synchronized Throwable fillInStackTrace()
	{
		return this;
	}
}
