package solidbase.spring;

import javax.sql.DataSource;

/**
 * Secondary connections used by the {@link UpgradeBean}.
 *
 * @author René M. de Bloois
 */
public class SecondaryConnection
{
	/**
	 * The configured name of the secondary connection.
	 */
	protected String name;

	/**
	 * The configured database driver of the secondary connection.
	 */
	protected String driver;

	/**
	 * The configured database url of the secondary connection.
	 */
	protected String url;

	/**
	 * The configured datasource of the secondary connection.
	 */
	protected DataSource datasource;

	/**
	 * The configured user name of the secondary connection.
	 */
	protected String username;

	/**
	 * The configured password of the secondary connection.
	 */
	protected String password;

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
	 * Sets the name of the secondary connection to configure.
	 *
	 * @param name The name of the secondary connection to configure.
	 */
	public void setName( String name )
	{
		this.name = name;
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
	 * Sets the database driver of the secondary connection to configure.
	 *
	 * @param driver The database driver of the secondary connection to configure.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
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
	 * Sets the database url of the secondary connection to configure.
	 *
	 * @param url The database url of the secondary connection to configure.
	 */
	public void setUrl( String url )
	{
		this.url = url;
	}

	/**
	 * Returns the data source.
	 *
	 * @return The data source.
	 */
	public DataSource getDatasource()
	{
		return this.datasource;
	}

	/**
	 * Sets the data source.
	 *
	 * @param datasource the data source.
	 */
	public void setDatasource( DataSource datasource )
	{
		this.datasource = datasource;
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
	 * Sets the user name of the secondary connection to configure.
	 *
	 * @param username The user name of the secondary connection to configure.
	 */
	public void setUsername( String username )
	{
		this.username = username;
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

	/**
	 * Sets the password of the secondary connection to configure.
	 *
	 * @param password The password of the secondary connection to configure.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}
}
