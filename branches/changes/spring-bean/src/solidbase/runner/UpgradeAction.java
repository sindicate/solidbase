package solidbase.runner;

import solidbase.core.Database;
import solidbase.core.Factory;
import solidbase.core.PatchProcessor;
import solidbase.util.Resource;

public class UpgradeAction implements Action
{
	protected Resource upgradeFile;
	protected String target;
	protected boolean downgradeAllowed;

	public UpgradeAction( Resource upgradeFile, String target, boolean downgradeAllowed )
	{
		this.upgradeFile = upgradeFile;
		this.target = target;
		this.downgradeAllowed = downgradeAllowed;
	}

	public void execute( Runner runner )
	{
		if( runner.listener == null )
			throw new IllegalStateException( "ProgressListener not set" );

		PatchProcessor processor = new PatchProcessor( runner.listener );

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

		processor.setPatchFile( Factory.openPatchFile( this.upgradeFile, runner.listener ) );
		try
		{
			processor.init();
			runner.listener.println( "Connecting to database..." );
			runner.listener.println( processor.getVersionStatement() );
			processor.patch( this.target, this.downgradeAllowed ); // TODO Print this target
			runner.listener.println( "" );
			runner.listener.println( processor.getVersionStatement() );
		}
		finally
		{
			processor.end();
		}
	}
}
