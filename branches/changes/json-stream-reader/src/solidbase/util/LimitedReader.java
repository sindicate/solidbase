package solidbase.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class LimitedReader extends FilterReader
{
	private int limit;

	public LimitedReader( Reader parent, int limit )
	{
		super( parent );
		this.limit = limit;
	}

	@Override
	public int read() throws IOException
	{
		if( this.limit <= 0 )
			return -1;
		this.limit--;
		return super.read();
	}

	// TODO Implement the other read() methods

	@Override
	public void close() throws IOException
	{
		// Don't close
	}
}
