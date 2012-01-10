package solidbase.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class IndexedInputStream extends FilterInputStream
{
	private int index;

	public IndexedInputStream( InputStream parent )
	{
		super( parent );
	}

	@Override
	// TODO Implement the other reads to
	public int read() throws IOException
	{
		this.index++;
		return super.read();
	}

	// TODO Need better implementation
	public void skipTo( int index ) throws IOException
	{
		while( this.index < index )
			read();
	}
}
