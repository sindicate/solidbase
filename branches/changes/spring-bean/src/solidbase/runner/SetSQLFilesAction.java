package solidbase.runner;

import java.util.List;

import solidbase.util.Resource;


public class SetSQLFilesAction implements Action
{
	protected List< Resource > sqlFiles;

	public SetSQLFilesAction( List< Resource > sqlFiles )
	{
		this.sqlFiles = sqlFiles;
	}

	public void execute( Runner runner )
	{
		runner.sqlFiles = this.sqlFiles;
	}
}
