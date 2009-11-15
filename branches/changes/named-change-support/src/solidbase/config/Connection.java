package solidbase.config;

public class Connection
{
	protected String name;
	protected String driver;
	protected String url;
	protected String user;
	protected String password;

	// TODO Rename user to username everywhere
	public Connection( String name, String driver, String url, String user, String password )
	{
		this.name = name;
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public String getName()
	{
		return this.name;
	}

	public String getDriver()
	{
		return this.driver;
	}

	public String getUrl()
	{
		return this.url;
	}

	public String getUser()
	{
		return this.user;
	}

	public String getPassword()
	{
		return this.password;
	}
}
