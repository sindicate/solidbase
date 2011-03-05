package solidbase.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;

import solidbase.Console;
import solidbase.Progress;
import solidbase.Version;
import solidbase.core.Database;
import solidbase.core.PatchProcessor;
import solidbase.core.Factory;
import solidbase.core.SystemException;

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

	public void upgrade()
	{
//		validate(); TODO

		Progress progress = new Progress( new Console(), false );

		String info = Version.getInfo();
		progress.println( info );
		progress.println( "" );

		Database database;
		if( this.datasource != null )
			database = new Database( "default", this.datasource, this.username, this.password, progress );
		else
			database = new Database( "default", this.driver, this.url, this.username, this.password, progress );
		PatchProcessor processor = new PatchProcessor( progress, database );

//			for( Connection connection : this.connections )
//				processor.addDatabase(
//						new Database( connection.getName(), connection.getDriver() == null ? this.driver : connection.getDriver(),
//								connection.getUrl() == null ? this.url : connection.getUrl(),
//										connection.getUsername(), connection.getPassword(), progress ) );

		try
		{
			processor.setPatchFile( Factory.openPatchFile( this.upgradefile.getURL(), progress ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e.getMessage() );
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
