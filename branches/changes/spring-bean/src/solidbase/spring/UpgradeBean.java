package solidbase.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.Factory;
import solidbase.core.PatchProcessor;
import solidbase.core.SystemException;
import solidbase.util.DriverDataSource;
import solidbase.util.MemoryResource;
import solidbase.util.URLResource;

public class UpgradeBean
{
	private String driver;
	private String url;
	private DataSource datasource;
	private String username;
	private String password;
	private Resource upgradefile;
	private String target;
	private List< SecondaryConnection > secondary = new ArrayList< SecondaryConnection >();

	public String getDriver()
	{
		return this.driver;
	}

	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	public String getUrl()
	{
		return this.url;
	}

	public void setUrl( String url )
	{
		this.url = url;
	}

	public DataSource getDatasource()
	{
		return this.datasource;
	}

	public void setDatasource( DataSource datasource )
	{
		this.datasource = datasource;
	}

	public String getUsername()
	{
		return this.username;
	}

	public void setUsername( String username )
	{
		this.username = username;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void setPassword( String password )
	{
		this.password = password;
	}

	public Resource getUpgradefile()
	{
		return this.upgradefile;
	}

	public void setUpgradefile( Resource upgradefile )
	{
		this.upgradefile = upgradefile;
	}

	public String getTarget()
	{
		return this.target;
	}

	public void setTarget( String target )
	{
		this.target = target;
	}

	public List< SecondaryConnection > getSecondary()
	{
		return this.secondary;
	}

	public void setSecondary( List< SecondaryConnection > secondary )
	{
		this.secondary = secondary;
	}


	/**
	 * Validates the configuration of the Upgrade bean.
	 */
	protected void validate()
	{
		if( this.datasource == null )
		{
			Assert.hasText( this.driver, "Missing 'datasource' or 'driver' for " + getClass().getName() );
			Assert.hasText( this.url, "Missing 'datasource' or 'url' for " + getClass().getName() );
			Assert.notNull( this.username, "Missing 'username' for " + getClass().getName() );
			Assert.notNull( this.password, "Missing 'password' for " + getClass().getName() );
		}

		for( SecondaryConnection connection : this.secondary )
			if( connection.getDatasource() == null )
			{
				Assert.hasText( connection.getName(), "Missing 'name' for " + connection.getClass().getName() );
				Assert.isTrue( !connection.getName().equals( "default" ), "The connection name 'default' is reserved" );
				Assert.notNull( connection.getUsername(), "Missing 'username' for " + connection.getClass().getName() );
				Assert.notNull( connection.getPassword(), "Missing 'password' for " + connection.getClass().getName() );
			}
	}

	public void upgrade()
	{
		validate();

		ProgressLogger progress = new ProgressLogger();

		String info = Version.getInfo();
		progress.println( info );
		progress.println( "" );

		DataSource dataSource = this.datasource;
		if( dataSource == null )
			dataSource = new DriverDataSource( this.driver, this.url, this.username, this.password );
		Database database = new Database( "default", dataSource, this.username, this.password, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );

		for( SecondaryConnection secondary : this.secondary )
		{
			DataSource secondaryDataSource = secondary.getDatasource();
			if( secondaryDataSource == null )
				if( secondary.getDriver() != null )
					secondaryDataSource = new DriverDataSource( secondary.getDriver(), secondary.getUrl(), secondary.getUsername(), secondary.getPassword() );
				else
					secondaryDataSource = dataSource;
			processor.addDatabase( new Database( secondary.getName(), secondaryDataSource, secondary.getUsername(), secondary.getPassword(), progress ) );
		}

		try
		{
			solidbase.util.Resource resource;
			if( this.upgradefile instanceof ByteArrayResource )
				resource = new MemoryResource( ( (ByteArrayResource)this.upgradefile ).getByteArray() );
			else if( this.upgradefile.isOpen() )
				// Open means that the resource cannot be reopened. Thats why we read it into memory.
				resource = new MemoryResource( this.upgradefile.getInputStream() );
			else
				resource = new URLResource( this.upgradefile.getURL() );
			processor.setPatchFile( Factory.openPatchFile( resource, progress ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		try
		{
			processor.init();
			progress.println( "Connecting to database..." );
			progress.println( processor.getVersionStatement() );
			processor.patch( this.target, false );
			progress.println( "" );
			progress.println( processor.getVersionStatement() );
		}
		finally
		{
			processor.end();
		}
	}
}
