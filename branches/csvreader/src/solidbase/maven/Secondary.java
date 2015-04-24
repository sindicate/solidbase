package solidbase.maven;


/**
 * A secondary connection used during configuration of the Maven Plugin.
 * 
 * @author René M. de Bloois
 */
public class Secondary
{
	private String name;
	private String driver;
	private String url;
	private String username;
	private String password;

	/**
	 * Constructor.
	 */
	public Secondary()
	{
		super();
	}

	/**
	 * Returns the configured name of the secondary connection.
	 * 
	 * @return The configured name of the secondary connection.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the configured database driver of the secondary connection.
	 * 
	 * @return The configured database driver of the secondary connection.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Returns the configured database url of the secondary connection.
	 * 
	 * @return The configured database url of the secondary connection.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Returns the configured user name of the secondary connection.
	 * 
	 * @return The configured user name of the secondary connection.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Returns the configured password of the secondary connection.
	 * 
	 * @return The configured password of the secondary connection.
	 */
	public String getPassword()
	{
		return this.password;
	}
}
