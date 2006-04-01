package ronnie.dbpatcher.core;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
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
