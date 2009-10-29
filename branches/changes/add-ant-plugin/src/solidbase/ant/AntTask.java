package solidbase.ant;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import solidbase.ant.AntTask.Database.Connection;
import solidbase.config.Configuration;
import solidbase.core.Patcher;


// TODO Rename this class
public class AntTask extends Task
{
	protected List< Database > databases = new ArrayList< Database >();

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

	// TODO Split this into logic for the message and the printing itself
	static protected void printCurrentVersion( Progress progress )
	{
		String version = Patcher.getCurrentVersion();
		String target = Patcher.getCurrentTarget();
		int statements = Patcher.getCurrentStatements();

		if( version == null )
		{
			if( target != null )
				progress.info( "The database has no version yet, incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
			else
				progress.info( "The database has no version yet." );
		}
		else
		{
			if( target != null )
				progress.info( "Current database version is \"" + version + "\", incompletely patched to version \"" + target + "\" (" + statements + " statements successful)." );
			else
				progress.info( "Current database version is \"" + version + "\"." );
		}
	}

	@Override
	public void execute()
	{
		//log( getClasspath().toString() );

		// TODO Validate the xml structure + exactly 1 database

		Project project = getProject();
		Progress progress = new Progress( getProject(), this );
		Configuration configuration = new Configuration( progress );

		progress.info( "SolidBase v" + configuration.getVersion() );
		progress.info( "(C) 2006-2009 René M. de Bloois" );
		progress.info( "" );

		Patcher.setCallBack( progress );

		Database database = this.databases.get( 0 );
		Patcher.setDefaultConnection( new solidbase.core.Database( database.getDriver(), database.getUrl(), database.getUser(), database.getPassword() ) );

		for( Connection connection : database.getConnections() )
			Patcher.addConnection( new solidbase.config.Connection( connection.getName(), connection.getDriver(), connection.getUrl(), connection.getUser(), connection.getPassword() ) );

		String patchFile = database.getPatchFile();
		String target = database.getTarget();

		progress.info( "Connecting to database '" + database.getName() + "'..." );

		printCurrentVersion( progress );

		try
		{
			Patcher.openPatchFile( patchFile );
			try
			{
				if( target != null )
					Patcher.patch( target ); // TODO Print this target
				else
					throw new UnsupportedOperationException();
				progress.info( "" );
				printCurrentVersion( progress );
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
