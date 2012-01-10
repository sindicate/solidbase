package solidbase.util;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream
{
	private InputStream parent;
	private int limit;

	public LimitedInputStream( InputStream parent, int limit )
	{
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public int read() throws IOException
	{
		if( this.limit <= 0 )
			return -1;
		this.limit--;
		return this.parent.read();
	}

	// TODO Implement the other read() methods
}
