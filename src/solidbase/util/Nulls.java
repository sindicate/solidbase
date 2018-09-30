package solidbase.util;

/**
 * Null checks.
 */
public class Nulls
{
	/**
	 * Checks for null and returns the given object. Throws a {@link NullPointerException} when the given object is
	 * null.
	 *
	 * @param t The object that needs to be checked for null.
	 * @return The object.
	 * @throws NullPointerException When the given object is found to be null.
	 */
	public static <T> T nonNull( T t ) {
		if( t == null ) {
			throw new NullPointerException();
		}
		return t;
	}

}
