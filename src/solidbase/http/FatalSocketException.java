package solidbase.http;

public class FatalSocketException extends RuntimeException
{
	public FatalSocketException( String message, Throwable cause )
	{
		super( message, cause );
	}

	public FatalSocketException( String message )
	{
		super( message );
	}

	public FatalSocketException( Throwable cause )
	{
		super( cause );
	}
}
