package solidbase.maven;

public class Secondary
{
	private String name;
	private String driver;
	private String url;
	private String username;
	private String password;

	public Secondary()
	{
		System.out.println( "Secondary constructor" );
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

	public String getUsername()
	{
		return this.username;
	}

	public String getPassword()
	{
		return this.password;
	}
}
