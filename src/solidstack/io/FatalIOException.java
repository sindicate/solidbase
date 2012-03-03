package solidstack.io;

public class FatalIOException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

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
