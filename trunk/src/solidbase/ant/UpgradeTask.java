package solidbase.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import solidbase.Main;
import solidbase.config.Configuration;
import solidbase.core.Patcher;
import solidbase.core.SQLExecutionException;


// TODO Rename this class
public class UpgradeTask extends Task
{
	protected String name;
	protected String driver;
	protected String url;
	protected String user;
	protected String password;
	protected String patchfile;
	protected String target;

	protected List< Connection > connections = new ArrayList< Connection >();

	/*
	protected Path classpath;

	public Path createClasspath()
	{
		if( this.classpath == null )
			this.classpath = new Path( getProject() );
		return this.classpath.createPath();
	}

	public Path getClasspath()
	{
		return this.classpath;
	}

	public void setClasspath( Path classpath )
	{
		if( this.classpath == null )
			this.classpath = new Path( getProject() );
		this.classpath.append( classpath );
	}

	public void setClasspathref( Reference reference )
	{
		if( this.classpath == null )
			this.classpath = new Path( getProject() );
		this.classpath.createPath().setRefid( reference );
	}
	 */

	public String getName()
	{
		return this.name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

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

	public String getUser()
	{
		return this.user;
	}

	public void setUser( String user )
	{
		this.user = user;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void setPassword( String password )
	{
		this.password = password;
	}

	public String getPatchfile()
	{
		return this.patchfile;
	}

	public void setPatchfile( String patchfile )
	{
		this.patchfile = patchfile;
	}

	public String getTarget()
	{
		return this.target;
	}

	public void setTarget( String target )
	{
		this.target = target;
	}

	public Connection createSecondary()
	{
		Connection connection = new Connection();
		this.connections.add( connection );
		return connection;
	}

	public List< Connection > getConnections()
	{
		return this.connections;
	}

	protected class Connection
	{
		protected String name;
		protected String driver;
		protected String url;
		protected String user;
		protected String password;

		public String getName()
		{
			return this.name;
		}

		public void setName( String name )
		{
			this.name = name;
		}

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

		public String getUser()
		{
			return this.user;
		}

		public void setUser( String user )
		{
			this.user = user;
		}

		public String getPassword()
		{
			return this.password;
		}

		public void setPassword( String password )
		{
			this.password = password;
		}
	}


	protected void validate()
	{
		if( this.driver == null )
			throw new BuildException( "The 'driver' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.url == null )
			throw new BuildException( "The 'url' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.user == null )
			throw new BuildException( "The 'user' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.password == null )
			throw new BuildException( "The 'password' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.patchfile == null )
			throw new BuildException( "The 'patchfile' attribute is mandatory for the " + getTaskName() + " task" );
		if( this.target == null )
			throw new BuildException( "The 'target' attribute is mandatory for the " + getTaskName() + " task" );

		for( Connection connection : this.connections )
		{
			if( connection.getName() == null )
				throw new BuildException( "The 'name' attribute is mandatory for a 'connection' element" );
			if( connection.getUser() == null )
				throw new BuildException( "The 'user' attribute is mandatory for a 'connection' element" );
			if( connection.getPassword() == null )
				throw new BuildException( "The 'password' attribute is mandatory for a 'connection' element" );
			if( connection.getName().equals( "default" ) )
				throw new BuildException( "The connection name 'default' is reserved" );
		}
	}


	@Override
	public void execute()
	{
		validate();

		Progress progress = new Progress( getProject(), this );
		Configuration configuration = new Configuration( progress );

		progress.info( "SolidBase v" + configuration.getVersion() );
		progress.info( "(C) 2006-2009 René M. de Bloois" );
		progress.info( "" );

		Patcher.setCallBack( progress );

		Patcher.setDefaultConnection( new solidbase.core.Database( this.driver, this.url, this.user, this.password ) );

		for( Connection connection : this.connections )
			Patcher.addConnection( new solidbase.config.Connection( connection.getName(), connection.getDriver(), connection.getUrl(), connection.getUser(), connection.getPassword() ) );

		progress.info( "Connecting to database..." );

		progress.info( Main.getCurrentVersion() );

		try
		{
			Patcher.openPatchFile( this.patchfile );
			try
			{
				if( this.target != null )
					Patcher.patch( this.target ); // TODO Print this target
				else
					throw new UnsupportedOperationException();
				progress.info( "" );
				progress.info( Main.getCurrentVersion() );
			}
			finally
			{
				Patcher.closePatchFile();
			}
		}
		catch( SQLExecutionException e )
		{
			throw new BuildException( e.getMessage() );
		}
	}
}
