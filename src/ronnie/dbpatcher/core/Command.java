package ronnie.dbpatcher.core;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class Command
{
	protected String command;
	protected boolean counting;
	
	public boolean isCounting()
	{
		return this.counting;
	}

	public String getCommand()
	{
		return this.command;
	}

	protected Command( String command, boolean counting )
	{
		this.command = command;
		this.counting = counting;
	}
}
