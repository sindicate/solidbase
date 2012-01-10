package solidbase.util;

import java.io.FilterReader;
import java.io.IOException;

public class LimitedReader extends FilterReader
{
	private IndexedReader parent;
	private int start;
	private int length;
	private boolean started;

	public LimitedReader( IndexedReader parent, int start, int length )
	{
		super( parent );
		this.parent = parent;
		this.start = start;
		this.length = length;
	}

	@Override
	public int read() throws IOException
	{
//		System.out.println( "read" );
		if( !this.started )
		{
			System.out.println( "LimitedReader starts at " + this.start );
			this.parent.skipTo( this.start );
			this.started = true;
		}
		if( this.length <= 0 )
		{
			System.out.println( "limit reached: " );
			return -1;
		}
		int result = super.read();
		Assert.isTrue( result != -1, "Can't read" );
		this.length--;
		return result;
	}

	@Override
	public int read( char[] cbuf, int off, int len ) throws IOException
	{
		System.out.println( "read " + len );
		int count = 0;
		while( len > 0 )
		{
			int c = read();
			if( c == -1 )
				return count > 0 ? count : -1;
			cbuf[ off++ ] = (char)c;
			len--;
			count++;
		}
		return count;
	}

	@Override
	public void close() throws IOException
	{
		// Don't close
	}

	@Override
	public void mark( int readAheadLimit ) throws IOException
	{
		throw new UnsupportedOperationException();
	}

//	@Override
//	public boolean markSupported()
//	{
//		throw new UnsupportedOperationException();
//	}

	@Override
	public boolean ready() throws IOException
	{
//		boolean result = super.ready();
//		Assert.isTrue( result );
//		return result;
		return true;
	}

	@Override
	public void reset() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public long skip( long n ) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
