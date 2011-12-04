package solidbase.test.ant;

import org.apache.tools.ant.Main;

public class AntMain extends Main
{
	public int exitCode;

	@Override
	protected void exit( int exitCode )
	{
		// Don't call exit, store the exitCode
		this.exitCode = exitCode;
	}
}
