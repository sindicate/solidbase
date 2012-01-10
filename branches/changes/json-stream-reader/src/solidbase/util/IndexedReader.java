package solidbase.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class IndexedReader extends FilterReader
{
	private int index;

	public IndexedReader( Reader parent )
	{
		super( parent );
	}

	@Override
	// TODO Implement the other reads to
	public int read() throws IOException
	{
		int result = super.read();
		if( result >= 0 )
			this.index++;
		return result;
	}

	// TODO Need better implementation
	public void skipTo( int index ) throws IOException
	{
		while( this.index < index )
		{
			int c = read();
			Assert.isTrue( c != -1, "Can't skip to " + index );
			this.index++;
		}
	}
}
