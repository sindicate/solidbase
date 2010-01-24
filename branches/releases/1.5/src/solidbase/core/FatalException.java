package solidbase.core;

public class FatalException extends RuntimeException
{
	/**
	 * A fatal exception is always caused by an explicit check in the code. So it is a situation which is expected but
	 * is not recoverable. No stacktrace should be presented to the user.
	 * 
	 * @param message The message for the user.
	 */
	public FatalException( String message )
	{
		super( message );
	}
}
