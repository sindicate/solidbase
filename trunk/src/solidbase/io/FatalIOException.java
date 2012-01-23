package solidbase.io;

public class FatalIOException extends RuntimeException
{
	public FatalIOException( String message, Throwable cause )
	{
		super( message, cause );
	}

	public FatalIOException( String message )
	{
		super( message );
	}

	public FatalIOException( Throwable cause )
	{
		super( cause );
	}
}
