package ronnie.dbpatcher;

public class Command
{
	protected String command;
	protected boolean internal;
	
	public boolean isInternal()
	{
		return this.internal;
	}

	public String getCommand()
	{
		return this.command;
	}

	protected Command( String command, boolean internal )
	{
		this.command = command;
		this.internal = internal;
	}
}
