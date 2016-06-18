package solidstack.cbor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Stack;

import org.apache.commons.collections.primitives.ArrayLongList;

import solidstack.cbor.Token.TYPE;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceInputStream;
import solidstack.io.SourceLocation;


public class CBORParser
{
	static private enum STATE { IBYTES, ITEXT, IARRAYMAP, BYTES, TEXT, ARRAYMAP };

	CBORScanner in;

	private ReverseByteStringIndex index;
	private STATE state;
	private Stack<StateItem> states = new Stack<StateItem>();

	private long remaining;


	public CBORParser( SourceInputStream in )
	{
		this.in = new CBORScanner( in );
	}

	public void close()
	{
		this.in.close();
	}

	public CBORScanner getScanner()
	{
		return this.in;
	}

	public Resource getResource()
	{
		return this.in.getResource();
	}

	public long getPos()
	{
		return this.in.getPos();
	}

	public Object getFromNamespace( int index, long pos )
	{
		ByteString result = this.index.get( index );
		if( result == null )
			throw new SourceException( "Illegal string ref: " + index, SourceLocation.forBinary( this.in.getResource(), pos ) );
		return result.toJava();
	}

	private Token get0()
	{
		CBORScanner in = this.in;

		SimpleToken t = in.get();
		if( !t.isTag() )
			return t;

		ArrayLongList tags = new ArrayLongList();
		while( t.isTag() )
		{
			tags.add( t.longValue() );
			t = in.get();
		}

		return t.withTags( tags.toArray() );
	}

	public Token get()
	{
		if( this.state == STATE.BYTES || this.state == STATE.TEXT )
			throw new IllegalStateException( "Bytes or text is waiting to be read" );

		long pos = this.in.getPos();

		Token t = get0();
		switch( t.type() )
		{
			case MAP:
			case ARRAY:
				checkNotIString( t, pos );
				decRemaining();
				pushState( STATE.ARRAYMAP );
				this.remaining = t.longValue() * ( t.type() == TYPE.MAP ? 2 : 1 );
				newNamespace( t );
				return t;

			case EOF:
				if( this.state != null )
					throw newSourceException( t, pos );
				return t;

			case IARRAY:
			case IMAP:
				// TODO Must count even amount of items if MAP
				checkNotIString( t, pos );
				decRemaining();
				pushState( STATE.IARRAYMAP );
				newNamespace( t );
				return t;

			case BYTES:
				if( this.state == STATE.ITEXT )
					throw newSourceException( t, pos );
				decRemaining();
				pushState( STATE.BYTES );
				this.remaining = t.longValue();
				newNamespace( t ); // Could be that they want to exclude this string from the current namespace
				return t;

			case TEXT:
				if( this.state == STATE.IBYTES )
					throw newSourceException( t, pos );
				decRemaining();
				pushState( STATE.TEXT );
				this.remaining = t.longValue();
				newNamespace( t ); // Could be that they want to exclude this string from the current namespace
				return t;

			case IBYTES:
				checkNotIString( t, pos );
				decRemaining();
				pushState( STATE.IBYTES );
				return t;

			case ITEXT:
				checkNotIString( t, pos );
				decRemaining();
				pushState( STATE.ITEXT );
				return t;

			case UINT:
			case DFLOAT:
			case BOOL:
			case NULL:
				checkNotIString( t, pos );
				decRemaining();
				return t;

			case BREAK:
				decRemaining();
				if( this.state != STATE.IARRAYMAP && this.state != STATE.IBYTES && this.state != STATE.ITEXT )
					throw newSourceException( t, pos );
				popState();
				return t;

			default:
				throw new UnsupportedOperationException( "Unexpected token: " + t );
		}
	}

	private void checkNotIString( Token token, long pos )
	{
		if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
			throw newSourceException( token, pos );
	}

	private SourceException newSourceException( Token token, long pos )
	{
		return new SourceException( "Unexpected " + token + " in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), pos ) );
	}

	private void decRemaining()
	{
		while( this.state == STATE.ARRAYMAP )
			if( this.remaining <= 0 )
				popState();
			else
			{
				this.remaining--;
				return;
			}
	}

	private void newNamespace( Token t )
	{
		if( t.hasTag( 0x102 ) )
			this.index = new SlidingReverseByteStringIndex( 10000, Integer.MAX_VALUE ); // FIXME Should come from the UINT
		else if( t.hasTag( 0x100 ) )
			this.index = new StandardReverseByteStringIndex();
	}

	public void readBytes( byte[] bytes )
	{
		if( this.state != STATE.BYTES )
			throw new SourceException( "Can't read bytes in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		this.in.readBytes( bytes );
		// TODO Check remaining
		popState();
		if( this.index != null )
			this.index.put( new ByteString( false, bytes ) );
	}

	public String readString( int len )
	{
		if( this.state != STATE.TEXT )
			throw new SourceException( "Can't read text in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		byte[] bytes = new byte[ len ];
		this.in.readBytes( bytes );
		// TODO Check remaining
		popState();
		if( this.index != null )
			this.index.put( new ByteString( true, bytes ) );
		return new String( bytes, CBORWriter.UTF8 );
	}

	void readBytesForStream( byte[] bytes )
	{
		this.in.readBytes( bytes );
		// TODO Check remaining
		popState();
	}

	public InputStream getInputStream()
	{
		if( this.state != STATE.ITEXT && this.state != STATE.IBYTES )
			throw new SourceException( "Can't stream bytes in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		return new BytesInputStream();
	}

	public Reader getReader()
	{
		if( this.state != STATE.ITEXT )
			throw new SourceException( "Can't stream text in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		return new InputStreamReader( new BytesInputStream(), CBORWriter.UTF8 );
	}

	private void pushState( STATE state )
	{
		this.states.push( new StateItem( this.state, this.index, this.remaining ) );
		this.state = state;
	}

	private void popState()
	{
		StateItem state = this.states.pop();
		if( state.state == STATE.ARRAYMAP && state.remaining == 0 )
			popState(); // again
		else
		{
			this.state = state.state;
			this.index = state.index;
			this.remaining = state.remaining;
		}
	}


	static private class StateItem
	{
		STATE state;
		ReverseByteStringIndex index;
		long remaining;

		StateItem( STATE state, ReverseByteStringIndex index, long remaining )
		{
			this.state = state;
			this.index = index;
			this.remaining = remaining;
		}
	}


	public class BytesInputStream extends InputStream
	{
		private byte[] buffer;
		private int pos;
		private boolean end;


		@Override
		public int read() throws IOException
		{
			if( this.end )
				return -1;

			int l;
			if( this.buffer == null || ( l = this.buffer.length - this.pos ) <= 0 )
				do
					l = fillBuffer();
				while( l == 0 );

			if( l < 0 )
				return -1;

			return this.buffer[ this.pos++ ];
		}

		@Override
		public int read( byte[] b, int off, int len ) throws IOException
		{
			if( this.end )
				return -1;

			int l;
			if( this.buffer == null || ( l = this.buffer.length - this.pos ) <= 0 )
				l = fillBuffer();

			if( l < 0 )
				return -1;

			if( len > l )
				len = l;
			System.arraycopy( this.buffer, this.pos, b, off, len );
			this.pos += len;
			return len;
		}

		private int fillBuffer()
		{
			Token t = get();
			if( t.type() == TYPE.BREAK )
			{
				this.end = true;
				return -1;
			}

			if( t.type() != TYPE.BYTES && t.type() != TYPE.TEXT ) // TODO Add the type to constructor
				throw new IllegalStateException( "Only byte or text strings allowed, not " + t.type() );

			this.buffer = new byte[ t.length() ];
			readBytesForStream( this.buffer );
			this.pos = 0;
			return t.length();
		}
	}
}
