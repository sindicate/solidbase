package solidstack.cbor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import solidstack.cbor.CBORScanner.Token;
import solidstack.cbor.CBORScanner.Token.TYPE;
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

	private boolean startNewNameSpace;
	private long remaining;


	public CBORParser( SourceInputStream in )
	{
		this.in = new CBORScanner( in );
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

	public Token get()
	{
		CBORScanner in = this.in;
		long pos = in.getPos();

		if( this.state == STATE.BYTES || this.state == STATE.TEXT )
			throw new IllegalStateException( "Bytes or text is waiting to be read" );

		Token t = in.get();
		TYPE type = t.type();
		switch( type )
		{
			case TAG:
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected TAG in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( t.longValue() == 0x100 )
				{
					this.startNewNameSpace = true;
					return get();
				}
				return t;

			case MAP:
			case ARRAY:
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected " + type + " in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.ARRAYMAP );
				this.remaining = t.longValue() * ( type == TYPE.MAP ? 2 : 1 );
				if( this.startNewNameSpace )
				{
					this.namespace = new ArrayList<Object>();
					this.startNewNameSpace = false;
				}
				return t;

			case EOF:
				if( this.state != null )
					throw new SourceException( "Unexpected EOF in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				return t;

			case IARRAY:
			case IMAP:
				// TODO Must count even amount of items if MAP
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected " + type + " in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.IARRAYMAP );
				if( this.startNewNameSpace )
				{
					this.namespace = new ArrayList<Object>();
					this.startNewNameSpace = false;
				}
				return t;

			case BYTES:
				if( this.state == STATE.ITEXT )
					throw new SourceException( "Unexpected BYTES in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.BYTES );
				this.remaining = t.longValue();
				if( this.startNewNameSpace )
				{
					this.namespace = new ArrayList<Object>();
					this.startNewNameSpace = false;
				}
				return t;

			case TEXT:
				if( this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected TEXT in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.TEXT );
				this.remaining = t.longValue();
				if( this.startNewNameSpace )
				{
					this.namespace = new ArrayList<Object>();
					this.startNewNameSpace = false;
				}
				return t;

				// Check state: namespace, state, startNewNameSpace, remaining
				// Change state: namespace, state, startNewNameSpace, remaining

			case IBYTES:
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected IBYTES in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.IBYTES );
				if( this.startNewNameSpace )
					this.startNewNameSpace = false;
				return t;

			case ITEXT:
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected ITEXT in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				}
				pushState( STATE.ITEXT );
				if( this.startNewNameSpace )
					this.startNewNameSpace = false;
				return t;

			case UINT:
			case DFLOAT:
			case BOOL:
				if( this.state == STATE.ITEXT || this.state == STATE.IBYTES )
					throw new SourceException( "Unexpected " + type + " in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				if( this.state == STATE.ARRAYMAP )
				{
					if( this.remaining == 0 )
						popState();
					this.remaining--;
//					if( this.remaining < 0 )
//						throw new SourceException( "Not enough items in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
//					if( this.remaining == 0 )
//						popState();
				}
				if( this.startNewNameSpace )
					this.startNewNameSpace = false;
				return t;

			case BREAK:
				if( this.state == STATE.ARRAYMAP )
					if( this.remaining == 0 )
						popState();
				if( this.state != STATE.IARRAYMAP && this.state != STATE.IBYTES && this.state != STATE.ITEXT )
					throw new SourceException( "Unexpected " + type + " in state: " + this.state, SourceLocation.forBinary( in.getResource(), pos ) );
				popState();
				if( this.startNewNameSpace )
					this.startNewNameSpace = false;
				return t;

			default:
				throw new UnsupportedOperationException( type.toString() );
		}
	}

	public void readBytes( byte[] bytes )
	{
		if( this.state != STATE.BYTES && this.state != STATE.TEXT )
			throw new SourceException( "Can't read bytes in state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		this.in.readBytes( bytes );
		// TODO Check remaining
		popState();
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
			throw new SourceException( "Can't read text in state: " + this.state, SourceLocation.forBinary( this.in.getResource(), this.in.getPos() ) );
		String s = this.in.readString( len );
		// TODO Check remaining
		popState();
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
