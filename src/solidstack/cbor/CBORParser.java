package solidstack.cbor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.collections.primitives.ArrayLongList;

import solidstack.cbor.CBORToken.TYPE;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceInputStream;
import solidstack.io.SourceLocation;


public class CBORParser
{
	static private enum STATE { IBYTES, ITEXT, IARRAYMAP, BYTES, TEXT, ARRAYMAP };

	private CBORScanner in;

	private List<Object> namespace;
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

	public Object getFromNamespace( int index )
	{
		return this.namespace.get( index );
	}

	private CBORToken get0()
	{
		CBORScanner in = this.in;

		CBORSimpleToken t = in.get();
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

	public CBORToken get()
	{
		if( this.state == STATE.BYTES || this.state == STATE.TEXT )
			throw new IllegalStateException( "Bytes or text is waiting to be read" );

		long pos = this.in.getPos();

		CBORToken t = get0();
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

	private void checkNotIString( CBORToken token, long pos )
	{
		if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
			throw newSourceException( token, pos );
	}

	private SourceException newSourceException( CBORToken token, long pos )
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

	private void newNamespace( CBORToken t )
	{
		if( t.hasTag( 0x100 ) )
			this.namespace = new ArrayList<Object>();
	}

	public void readBytes( byte[] bytes )
	{
		if( this.state != STATE.BYTES && this.state != STATE.TEXT )
			throw new SourceException( "Can't read bytes in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		this.in.readBytes( bytes );
		// TODO Check remaining
		popState();
		if( bytes.length <= CBORWriter.MAX_STRINGREF_LENGTH )
			if( this.namespace != null )
			{
				int index = this.namespace.size();
				if( bytes.length >= CBORWriter.getUIntSize( index ) + 2 )
					this.namespace.add( bytes );
			}
	}

	public String readString( int len )
	{
		if( this.state != STATE.TEXT )
			throw new SourceException( "Can't read text in parser state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		String s = this.in.readString( len );
		// TODO Check remaining
		popState();
		if( len <= CBORWriter.MAX_STRINGREF_LENGTH )
			if( this.namespace != null )
			{
				int index = this.namespace.size();
				if( len >= CBORWriter.getUIntSize( index ) + 2 )
					this.namespace.add( s );
			}
		return s;
	}

	private void pushState( STATE state )
	{
		this.states.push( new StateItem( this.state, this.namespace, this.remaining ) );
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
			this.namespace = state.namespace;
			this.remaining = state.remaining;
		}
	}

	static private class StateItem
	{
		STATE state;
		List<Object> namespace;
		long remaining;

		StateItem( STATE state, List<Object> namespace, long remaining )
		{
			this.state = state;
			this.namespace = namespace;
			this.remaining = remaining;
		}
	}
}
