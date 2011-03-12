package solidbase.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.Version;
import solidbase.core.ProgressListener;

public class Runner
{
	protected List< Action > steps = new ArrayList< Action >();
	protected ProgressListener listener;
	protected Map< String, Connection > connections = new HashMap< String, Connection >();

	public void step( Action step )
	{
		this.steps.add( step );
	}

//	public void setProgressListener( ProgressListener listener )
//	{
//		step( new SetProgressListener( listener ) );
//	}
//
//	public void setConnection( String name, String driverClassName, String url, String defaultUser, String defaultPassword )
//	{
//		step( new SetConnection( name, driverClassName, url, defaultUser, defaultPassword ) );
//	}
//
//	public void upgrade( Resource upgradeFile, String target, boolean downgradeAllowed )
//	{
//		step( new Upgrade( upgradeFile, target, downgradeAllowed ) );
//	}

	public void run()
	{
		boolean infoPrinted = false;
		for( Action step : this.steps )
		{
			step.execute( this );
			if( !infoPrinted && this.listener != null )
			{
				infoPrinted = true;
				this.listener.println( Version.getInfo() );
				this.listener.println( "" );
			}
		}
	}
}
