package solidbase;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Task;

import solidbase.config.Configuration;


public class AntTask extends Task
{
	protected List< Database > databases = new ArrayList< Database >();

	protected boolean verbose;
	protected boolean dumplog;

	protected Console console;

	public void setVerbose( boolean verbose )
	{
		this.verbose = verbose;
	}

	public void setDumplog( boolean dumplog )
	{
		this.dumplog = dumplog;
	}

	public Database createDatabase()
	{
		Database database = new Database();
		this.databases.add( database );
		return database;
	}

	protected class Database
	{
		protected String name;
		protected String driver;
		protected String url;
		protected String user;
		protected String password;
		protected String patchfile;
		protected String target;

		protected List< Connection > connections = new ArrayList< Connection >();

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

		public Connection createConnection()
		{
			Connection connection = new Connection();
			this.connections.add( connection );
			return connection;
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
	}

	@Override
	public void execute()
	{
		// TODO Validate the xml structure

		if( this.console == null )
			this.console = new Console();
		this.console.fromAnt = true;

		Progress progress = new Progress( this.console, this.verbose );
		Configuration configuration = new Configuration( progress );

		this.console.println( "SolidBase v" + configuration.getVersion() );
		this.console.println( "(C) 2006-2009 René M. de Bloois" );
		this.console.println();
	}
}
