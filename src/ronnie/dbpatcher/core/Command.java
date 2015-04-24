package ronnie.dbpatcher.core;

/**
 * <p>Represents a command in the patchfile. A command can be repeatable or non-repeatable. Repeatable commands start with <code>--*</code>. Example of a repeatable command is:</p>
 * <blockquote><pre>
 * --* SET USER SYSTEM</pre></blockquote>
 * <p>Repeatable commands are executed as they are encountered even when the patchtool is in the process of skipping non-repeatable commands.</p>
 * <p>Example of a non-repeatable command:</p>
 * <blockquote><pre>
 * DROP TABLE REJECTED_SHIPMENTS PURGE
 * GO</pre></blockquote>
 * <p>Non-repeatable commands are DDL (data definition language) most of the time.</p>
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
public class Command
{
	protected String command;
	protected boolean repeatable;

	/**
	 * Constructs the command.
	 * 
	 * @param command The text of the command.
	 * @param repeatable The repeatability of the command.
	 */
	protected Command( String command, boolean repeatable )
	{
		this.command = command;
		this.repeatable = repeatable;
	}

	/**
	 * Indicates if the command is repeatable or not.
	 * 
	 * @return true if the command is repeatable, false otherwise.
	 */
	public boolean isRepeatable()
	{
		return this.repeatable;
	}

	/**
	 * Indicates if the command is repeatable or not.
	 * 
	 * @return true if the command is non-repeatable, false otherwise.
	 */
	public boolean isNonRepeatable() // TODO Rename to isDatabaseCommand
	{
		return !this.repeatable;
	}

	/**
	 * Returns the text of the command.
	 * 
	 * @return the text of the command.
	 */
	public String getCommand()
	{
		return this.command;
	}
}
