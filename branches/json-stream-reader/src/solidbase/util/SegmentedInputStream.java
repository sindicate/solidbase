package solidbase.util;

import java.io.IOException;
import java.io.InputStream;

public class SegmentedInputStream extends InputStream
{
	protected InputStream parent;
	protected long index;
	protected long segmentEnd;

	public SegmentedInputStream( InputStream parent )
	{
		this.parent = parent;
	}

	@Override
	public int read() throws IOException
	{
		if( this.index >= this.segmentEnd )
			return -1;
		int result = this.parent.read();
		Assert.isTrue( result >= 0 );
		this.index ++;
		return result;
	}

	@Override
	public int read( byte[] b, int off, int len ) throws IOException
	{
//		System.out.println( "Read " + this.index );
		if( this.index >= this.segmentEnd )
			return -1;
		int read;
		if( len <= this.segmentEnd - this.index )
			read = this.parent.read( b, off, len );
		else
			read = this.parent.read( b, off, (int)( this.segmentEnd - this.index ) );
		Assert.isTrue( read >= 0 );
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
		Assert.isTrue( this.index <= start );
		this.parent.skip( start - this.index );
		this.index = start;
		this.segmentEnd = start + length;
	}

	public InputStream getSegmentInputStream( long start, long length )
	{
		return new SegmentInputStream( start, length );
	}

	public class SegmentInputStream extends InputStream
	{
		private long start;
		private long length;
		private boolean accessed;

		public SegmentInputStream( long start, long length )
		{
			this.start = start;
			this.length = length;
		}

		@Override
		public int read() throws IOException
		{
			if( !this.accessed )
			{
				gotoSegment( this.start, this.length );
				this.accessed = true;
			}
			return SegmentedInputStream.this.read();
		}

		@Override
		public int read( byte[] b, int off, int len ) throws IOException
		{
			if( !this.accessed )
			{
				gotoSegment( this.start, this.length );
				this.accessed = true;
			}
			return SegmentedInputStream.this.read( b, off, len );
		}

		@Override
		public void close() throws IOException
		{
			// No closing
		}
	}
}
