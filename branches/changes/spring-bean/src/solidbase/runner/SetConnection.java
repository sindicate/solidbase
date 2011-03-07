package solidbase.runner;


public class SetConnection implements Step
{
	protected String name;
	protected String driver;
	protected String url;
	protected String username;
	protected String password;

	public SetConnection( String name, String driver, String url, String username, String password )
	{
		this.name = name;
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public void execute( Runner runner )
	{
		runner.connections.put( this.name, new Connection( this.name, this.driver, this.url, this.username, this.password ) );
	}
}
