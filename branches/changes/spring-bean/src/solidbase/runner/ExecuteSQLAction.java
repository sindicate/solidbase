package solidbase.runner;

import solidbase.core.Database;
import solidbase.core.Factory;
import solidbase.core.SQLProcessor;
import solidbase.util.Resource;

public class ExecuteSQLAction implements Action
{
	public void execute( Runner runner )
	{
		if( runner.listener == null )
			throw new IllegalStateException( "ProgressListener not set" );

		SQLProcessor processor = new SQLProcessor( runner.listener );

		Connection def = runner.connections.get( "default" );
		if( def == null )
			throw new IllegalArgumentException( "Missing 'default' connection." );

		for( Connection connection : runner.connections.values() )
			processor.addDatabase(
					new Database(
							connection.getName(),
							connection.getDriver() == null ? def.driver : connection.getDriver(),
							connection.getUrl() == null ? def.url : connection.getUrl(),
							connection.getUsername(),
							connection.getPassword(),
							runner.listener
					)
			);

		try
		{
			boolean first = true;
			for( Resource resource : runner.sqlFiles )
			{
				processor.setSQLSource( Factory.openSQLFile( resource, runner.listener ).getSource() );
				if( first )
				{
					runner.listener.println( "Connecting to database..." ); // TODO Let the database say that (for example the default connection)
					first = false;
				}
				processor.process();
			}
		}
		finally
		{
			processor.end();
		}

		runner.listener.println( "" );
	}
}
