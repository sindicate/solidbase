/**
 * 
 */
package ronnie.dbpatcher.config;

import java.util.ArrayList;
import java.util.List;


import com.logicacmg.idt.commons.SystemException;

public class Database
{
	protected String name;
	protected String description;
	protected String driver;
	protected String url;
	protected List< Application > applications = new ArrayList();

	public Database( String name, String description, String driver, String url )
	{
		this.name = name;
		this.description = description;
		this.driver = driver;
		this.url = url;
	}

	public void addApplication( String name, String description, String userName, String patchFile )
	{
		this.applications.add( new Application( name, description, userName, patchFile ) );
	}

	public Application getApplication( String name )
	{
		for( Application application : this.applications )
			if( application.name.equals( name ) )
				return application;
		throw new SystemException( "Application [" + name + "] not configured for database [" + this.name + "]." );
	}

	public String getName()
	{
		return this.name;
	}

	public String getDescription()
	{
		return this.description;
	}

	public String getDriver()
	{
		return this.driver;
	}

	public String getUrl()
	{
		return this.url;
	}

	public List< Application > getApplications()
	{
		return this.applications;
	}
}