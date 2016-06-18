package solidstack.cbor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import solidstack.io.FatalIOException;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;


public class CBORWriter extends OutputStream
{
//	static public final int MAX_STRINGREF_LENGTH = 64;

	static private enum STATE { ARRAYMAP, IARRAYMAP, IBYTES, ITEXT,  };

	static final Charset UTF8 = Charset.forName( "UTF-8" );

	private OutputStream out;

	private ByteStringIndex index;

	private boolean startNewNameSpace;
	private boolean startNewSlidingNameSpace;

	private STATE state;
	private Stack<StateItem> stateStack = new Stack<StateItem>();


	public CBORWriter( OutputStream out )
	{
		this.out = out;
	}

	public ByteStringIndex getIndex()
	{
		return this.index;
	}

	// ----- OutputStream methods

	@Override
	public void write( int b ) throws IOException
	{
		this.out.write( b );
	}

	@Override
	public void write( byte[] b ) throws IOException
	{
		this.out.write( b );
	}

	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		this.out.write( b, off, len );
	}

	@Override
	public void flush() throws IOException
	{
		this.out.flush();
	}

	@Override
	public void close()
	{
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	// ----- CBOR methods

	public void writeIntU( int i )
	{
		checkState();
		writeUInt( 0x00, i );
		clearFlags();
	}

	public void writeIntU( long l )
	{
		checkState();
		writeUInt( 0x00, l );
		clearFlags();
	}

	// The given int is minus the real int minus 1. Example -500 becomes 500-1 = 499
	public void writeIntN( int i )
	{
		checkState();
		writeUInt( 0x20, i );
		clearFlags();
	}

	// The given int is minus the real int minus 1. Example -500 becomes 500-1 = 499
	public void writeIntN( long l )
	{
		checkState();
		writeUInt( 0x20, l );
		clearFlags();
	}

	public void writeBytes( byte[] bytes )
	{
		if( this.state == STATE.ITEXT )
			throw new IllegalStateException( "Only text strings allowed" );
		writeString( new ByteString( false, bytes ), 0x40 );
	}

	public void writeBytes( byte[] bytes, int offset, int len )
	{
		if( this.state == STATE.ITEXT )
			throw new IllegalStateException( "Only text strings allowed" );
		byte[] b = new byte[ len ];
		System.arraycopy( bytes, offset, b, 0, len );
		writeString( new ByteString( false, b ), 0x40 );
	}

	public void writeBytes( InputStream in )
	{
		if( this.state == STATE.ITEXT )
			throw new IllegalStateException( "Only text strings allowed" );

		startBytes();

		// TODO If smaller than buffer, than write normal
		byte[] buffer = new byte[ 4096 ]; // TODO ThreadLocal buffer?
		try
		{
			for( int read = in.read( buffer ); read >= 0; read = in.read( buffer ) )
				writeBytes( buffer, 0, read );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}

		writeBreak();
	}

	// TODO No refs
	public void startBytes()
	{
		checkState();
		pushState( STATE.IBYTES );
		writeByte( 0x5F ); // End with break
	}

	public void writeText( String text )
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );
		writeString( new ByteString( true, text.getBytes( UTF8 ) ), 0x60 );
	}

	public void writeText( char[] text )
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );
		writeString( new ByteString( true, new String( text ).getBytes( UTF8 ) ), 0x60 );
	}

	public void writeText( char[] text, int offset, int len )
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );
		writeString( new ByteString( true, new String( text, offset, len ).getBytes( UTF8 ) ), 0x60 );
	}

	public void writeText( Reader reader )
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );

		startText();

		// TODO If smaller than buffer, than write normal
		char[] buffer = new char[ 4096 ]; // TODO ThreadLocal buffer?
		try
		{
			for( int read = reader.read( buffer ); read >= 0; read = reader.read( buffer ) )
				writeText( buffer, 0, read );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}

		writeBreak();
	}

	public void startText()
	{
		checkState();
		pushState( STATE.ITEXT );
		writeByte( 0x7F ); // End with break
	}

	public void writeArray( String[] array )
	{
		checkState();
		pushState( STATE.ARRAYMAP );

		writeUInt( 0x80, array.length );
		for( String text : array )
			writeText( text );

		popState();
	}

	public void startArray( int length )
	{
		checkState();
		pushState( STATE.ARRAYMAP );
		writeUInt( 0x80, length );
	}

	public void startArray()
	{
		checkState();
		pushState( STATE.IARRAYMAP );
		writeByte( 0x9F ); // End with break
	}

	public void writeMap( Map<String, String> map )
	{
		checkState();
		pushState( STATE.ARRAYMAP );

		writeUInt( 0xA0, map.size() );
		for( Entry<String, String> entry : map.entrySet() )
		{
			writeText( entry.getKey() );
			writeText( entry.getValue() );
		}

		popState();
	}

	// TODO Check even amount of items
	public void startMap()
	{
		checkState();
		pushState( STATE.IARRAYMAP );

		writeByte( 0xBF ); // End with break
	}

	public void writeTag( int tag )
	{
		checkState();
		writeUInt( 0xC0, tag );
	}

	public void writeBoolean( boolean bool )
	{
		checkState();
		writeByte( bool ? 0xF5 : 0xF4 );
		clearFlags();
	}

	public void writeNull()
	{
		checkState();
		writeByte( 0xF6 );
		clearFlags();
	}

	public void writeUndefined()
	{
		checkState();
		writeByte( 0xF7 );
		clearFlags();
	}

	public void writeBreak()
	{
		writeByte( 0xFF );
		popState();
	}

	public void end()
	{
		if( this.state == STATE.IARRAYMAP )
			writeByte( 0xFF );
		popState();
	}

	// See http://stackoverflow.com/a/6162687/229140
	public void writeFloatH( float f )
	{
		checkState();
		throw new UnsupportedOperationException();
	}

	public void writeFloatS( float f )
	{
		checkState();

		writeByte( 0xFA );
		int i = Float.floatToIntBits( f );
		writeByte( i >>> 24 );
		writeByte( i >>> 16 );
		writeByte( i >>> 8 );
		writeByte( i );

		clearFlags();
	}

	public void writeFloatD( double d )
	{
		checkState();

		writeByte( 0xFB );
		long l = Double.doubleToLongBits( d );
		writeByte( (int)( l >>> 56 ) );
		writeByte( (int)( l >>> 48 ) );
		writeByte( (int)( l >>> 40 ) );
		writeByte( (int)( l >>> 32 ) );
		writeByte( (int)( l >>> 24 ) );
		writeByte( (int)( l >>> 16 ) );
		writeByte( (int)( l >>> 8 ) );
		writeByte( (int)l );

		clearFlags();
	}

	public void writeDateTime( Date value )
	{
		long millis = value.getTime();
		long seconds = millis / 1000;
		millis %= millis;
		if( millis != 0 )
			throw new UnsupportedOperationException();
		writeTag( 0x01 );
		if( seconds < 0 )
			writeIntN( -( seconds + 1 ) );
		else
			writeIntU( seconds );
	}

	public CBORWriter tagRefNS()
	{
		writeTag( 0x100 );
		this.startNewNameSpace = true;
		return this;
	}

	public CBORWriter tagSlidingRefNS()
	{
		writeTag( 0x102 ); // TODO Add capacity and maxItemLength
		this.startNewSlidingNameSpace = true;
		return this;
	}

	// TODO see Table 5: https://tools.ietf.org/html/rfc7049

	// ----- Higher level object and array methods

	public void writeInt( int i )
	{
		if( i < 0 )
			writeIntN( -( i + 1 ) );
		else
			writeIntU( i );
	}

	public void writeLong( long l )
	{
		if( l < 0 )
			writeIntN( -( l + 1 ) );
		else
			writeIntU( l );
	}

	public void write( Object object )
	{
		if( object instanceof String )
			writeText( (String)object );
		else if( object instanceof Integer )
			writeInt( (Integer)object );
		else if( object instanceof Long )
			writeLong( (Long)object );
		else if( object instanceof Date )
			writeDateTime( (Date)object );
		else if( object instanceof JSONObject )
			write( (JSONObject)object );
		else if( object instanceof JSONArray )
			write( (JSONArray)object );
		else
			throw new UnsupportedOperationException( object.getClass().getName() );
	}

	// TODO Merge with writeMap
	public void write( JSONObject object )
	{
		checkState();
		pushState( STATE.ARRAYMAP );

		writeUInt( 0xA0, object.size() );
		for( Entry<String, Object> entry : object )
		{
			writeText( entry.getKey() );
			write( entry.getValue() );
		}

		popState();
	}

	// TODO Merge with writeArray
	public void write( JSONArray array )
	{
		checkState();
		pushState( STATE.ARRAYMAP );

		writeUInt( 0x80, array.size() );
		for( Object object : array )
			write( object );

		popState();
	}

	// ----- Private methods

	private void writeByte( int b )
	{
		try
		{
			write( b );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	private void writeUInt( int major, int i )
	{
		if( i < 0 || i >= 0x10000 )
		{
			writeByte( major + 26 );
			writeByte( i >>> 24 );
			writeByte( i >>> 16 );
			writeByte( i >>> 8 );
			writeByte( i );
		}
		else if( i < 24 )
		{
			writeByte( major + i );
		}
		else if( i < 0x100 )
		{
			writeByte( major + 24 );
			writeByte( i );
		}
		else
		{
			writeByte( major + 25 );
			writeByte( i >>> 8 );
			writeByte( i );
		}
	}

	private void writeUInt( int major, long l )
	{
		if( l < 0 || l >= 0x100000000L )
		{
			writeByte( major + 27 );
			writeByte( (int)( l >>> 56 ) );
			writeByte( (int)( l >>> 48 ) );
			writeByte( (int)( l >>> 40 ) );
			writeByte( (int)( l >>> 32 ) );
			writeByte( (int)( l >>> 24 ) );
			writeByte( (int)( l >>> 16 ) );
			writeByte( (int)( l >>> 8 ) );
			writeByte( (int)l );
		}
		else
			writeUInt( major, (int)l );
	}

	static public int getUIntSize( int i )
	{
		if( i < 0 || i >= 0x10000 )
			return 5;
		if( i < 24 )
			return 1;
		if( i < 0x100 )
			return 2;
		return 3;
	}

	private void clearFlags()
	{
		if( this.startNewNameSpace )
			this.startNewNameSpace = false;
		if( this.startNewSlidingNameSpace )
			this.startNewSlidingNameSpace = false;
	}

	private void pushState( STATE state )
	{
		this.stateStack.push( new StateItem( this.state, this.index ) );
		this.state = state;

		if( this.startNewSlidingNameSpace )
		{
			this.index = new SlidingByteStringIndex( 10000, Integer.MAX_VALUE );
			this.startNewSlidingNameSpace = false;
		}
		if( this.startNewNameSpace )
		{
			this.index = new StandardByteStringIndex();
			this.startNewNameSpace = false;
		}
	}

	private void popState()
	{
		StateItem state = this.stateStack.pop();
		this.state = state.state;
		this.index = state.index;
	}

	private void checkState()
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );
		if( this.state == STATE.ITEXT )
			throw new IllegalStateException( "Only text strings allowed" );
	}

	private void writeString( ByteString bs, int major )
	{
		clearFlags();

//		if( bs.length() <= CBORWriter.MAX_STRINGREF_LENGTH )
			if( this.index != null && this.state != STATE.IBYTES && this.state != STATE.ITEXT )
			{
				Integer index = this.index.putOrGet( bs );
				if( index != null )
				{
					writeTag( 25 );
					writeIntU( index );
					return;
				}
			}

		writeBytes( major, bs.unwrap() );
	}

	// Should only be called by something that cares for the dictionary
	private void writeBytes( int major, byte[] bytes )
	{
		writeUInt( major, bytes.length );
		try
		{
			write( bytes );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	static private class StateItem
	{
		STATE state;
		ByteStringIndex index;

		StateItem( STATE state, ByteStringIndex index )
		{
			this.state = state;
			this.index = index;
		}
	}
}
