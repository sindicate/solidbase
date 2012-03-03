package solidstack.io;

import java.io.IOException;
import java.io.Reader;


public class SegmentedReader extends Reader
{
	protected Reader parent;
	protected long index;
	protected long segmentEnd;

	public SegmentedReader( Reader parent )
	{
		super( parent );
		this.parent = parent;
	}

	@Override
	public int read( char[] cbuf, int off, int len ) throws IOException
	{
//		System.out.println( "Read " + this.index );
		if( this.index >= this.segmentEnd )
			return -1;
		int read;
		if( len <= this.segmentEnd - this.index )
			read = this.parent.read( cbuf, off, len );
		else
			read = this.parent.read( cbuf, off, (int)( this.segmentEnd - this.index ) );
		if( read == -1 )
			throw new FatalIOException( "Segment not complete" );
		this.index += read;
		return read;
	}

	@Override
	public void close() throws IOException
	{
		this.parent.close();
	}

	public void gotoSegment( long start, long length ) throws IOException
	{
//		System.out.println( "Goto segment " + start );
		if( this.index > start )
			throw new FatalIOException( "Past segment" );
		this.parent.skip( start - this.index );
		this.index = start;
		this.segmentEnd = start + length;
	}

	public Reader getSegmentReader( long start, long length )
	{
		return new SegmentReader( start, length );
	}

	public class SegmentReader extends Reader
	{
		private long start;
		private long length;
		private boolean accessed;

		public SegmentReader( long start, long length )
		{
			this.start = start;
			this.length = length;
		}

		@Override
		public int read( char[] cbuf, int off, int len ) throws IOException
		{
			if( !this.accessed )
			{
				gotoSegment( this.start, this.length );
				this.accessed = true;
			}
			return SegmentedReader.this.read( cbuf, off, len );
		}

		@Override
		public void close() throws IOException
		{
			// No closing
		}
	}
}
