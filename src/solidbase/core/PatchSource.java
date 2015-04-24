package solidbase.core;

import solidbase.util.LineReader;


/**
 * The source for command from a patch file.
 * 
 * @author René M. de Bloois
 */
public class PatchSource extends SQLSource
{
	/**
	 * Creates a new instance of a patch source.
	 * 
	 * @param in The reader which is used to read the SQL.
	 */
	protected PatchSource( LineReader in )
	{
		super( in );
	}

	/**
	 * Reads a statement from the patch file.
	 * 
	 * @return A statement from the patch file or null when no more statements are available.
	 */
	@Override
	public Command readCommand()
	{
		Command command;

		do
		{
			command = super.readCommand();
			Assert.notNull( command, "Premature end of file found" );

			if( command.isTransient() )
			{
				if( PatchFile.PATCH_END_PATTERN.matcher( command.getCommand() ).matches() )
					return null;
				if( PatchFile.PATCH_START_PATTERN.matcher( command.getCommand() ).matches() )
					command = null;
			}
		}
		while( command == null );

		return command;
	}
}
