package solidbase.core.script;

import solidstack.io.SourceLocation;

/**
 * A fragment.
 */
public class Fragment
{
	private SourceLocation location;
	private String value;

	Fragment( SourceLocation location, String value )
	{
		this.location = location;
		this.value = value;
	}

	/**
	 * @return The location of the token in the source.
	 */
	public SourceLocation getLocation()
	{
		return this.location;
	}

	/**
	 * @return The value of the token.
	 */
	public String getValue()
	{
		return this.value;
	}

	public int length()
	{
		return this.value.length();
	}
}