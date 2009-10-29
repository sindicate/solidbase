package solidbase;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import solidbase.AntTask.Database.Connection;
import solidbase.config.Configuration;
import solidbase.core.Patcher;


public class AntTask extends Task
{
	protected List< Database > databases = new ArrayList< Database >();

	protected boolean verbose;

	protected Console console;

	public void setVerbose( boolean verbose )
	{
		this.verbose = verbose;
	}

	public Database createDatabase()
	{
		Database database = new Database();
		this.databases.add( database );
		return database;
	}

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

		public String getPatchFile()
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
	}

	@Override
	public void execute()
	{
		//log( getClasspath().toString() );

		// TODO Validate the xml structure + exactly 1 database

		if( this.console == null )
			this.console = new Console();
		this.console.fromAnt = true;

		Progress progress = new Progress( this.console, this.verbose );
		Configuration configuration = new Configuration( progress );

		this.console.println( "SolidBase v" + configuration.getVersion() );
		this.console.println( "(C) 2006-2009 René M. de Bloois" );
		this.console.println();

		Patcher.setCallBack( progress );

		Database database = this.databases.get( 0 );
		Patcher.setDefaultConnection( new solidbase.core.Database( database.getDriver(), database.getUrl(), database.getUser(), database.getPassword() ) );

		for( Connection connection : database.getConnections() )
			Patcher.addConnection( new solidbase.config.Connection( connection.getName(), connection.getDriver(), connection.getUrl(), connection.getUser(), connection.getPassword() ) );

		String patchFile = database.getPatchFile();
		String target = database.getTarget();

		this.console.println( "Connecting to database '" + database.getName() + "'..." );

		Main.printCurrentVersion( this.console );

		try
		{
			Patcher.openPatchFile( patchFile );
			try
			{
				if( target != null )
					Patcher.patch( target ); // TODO Print this target
				else
				{
					// Need linked set because order is important
					LinkedHashSet< String > targets = Patcher.getTargets( false, null );
					if( targets.size() > 0 )
					{
						this.console.println( "Possible targets are: " + Main.list( targets ) );
						this.console.print( "Input target version: " );
						String input = this.console.input();
						Patcher.patch( input );
					}
					else
						this.console.println( "There are no possible targets." );
					// TODO Distinguish between uptodate and no possible path
				}
				this.console.emptyLine();
				Main.printCurrentVersion( this.console );
			}
			finally
			{
				Patcher.closePatchFile();
			}
		}
		catch( IOException e )
		{
			throw new BuildException( e );
		}
		catch( SQLException e )
		{
			throw new BuildException( e );
		}
	}
}
