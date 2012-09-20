package solidbase.maven;

/**
 * Parameter object to allow configuring of parameters.
 *
 * @author René de Bloois
 */
public class Parameter
{
	private String name;
	private String value;

	/**
	 *
	 */
	public Parameter()
	{
	}

	/**
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 */
	public Parameter( String name, String value )
	{
		this.name = name;
		this.value = value;
	}

	/**
	 * @return The name of the parameter.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Sets the name of the parameter.
	 * @param name The name for the parameter.
	 */
	public void setName( String name )
	{
		this.name = name;
	}

	/**
	 * @return The value of the parameter.
	 */
	public String getValue()
	{
		return this.value;
	}

	/**
	 * Set the value of the parameter.
	 * @param value The value of the parameter.
	 */
	// TODO Text as value, but Ant does not substitute placeholders in the text element.
	public void setValue( String value )
	{
		this.value = value;
	}
}
