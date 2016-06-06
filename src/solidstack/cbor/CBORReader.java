package solidstack.cbor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;


public class CBORReader
{
	static private enum STATE { IBYTES, ITEXT, ARRAYMAP };

	private CBORScanner in;

	private List<Object> namespace;
	private STATE state;
	private Stack<StateItem> states = new Stack<StateItem>();

	private DiskBuffer disk = new DiskBuffer( "cborbuffer" );


	public CBORReader( InputStream in )
	{
		this.in = new CBORScanner( in );
	}

	public CBORScanner getCBORScanner()
	{
		return this.in;
	}

	public void close()
	{
//		this.in.close();
		this.disk.close();
	}

	public Token get()
	{
		return this.in.get();
	}

	public Object read()
	{
		CBORScanner in = this.in;
		Token t = in.get();
		TYPE type = t.getType();
		switch( type )
		{
			case TAG:
				checkState();
				if( t.getValue() == 0x19 )
				{
					t = in.get();
					if( t.getType() != TYPE.UINT )
						throw new CBORException( "Expected an UINT, not " + t.getType() );
					return this.namespace.get( t.getLength() );
				}
				else if( t.getValue() == 0x01 )
				{
					t = in.get();
					if( t.getType() != TYPE.UINT )
						throw new CBORException( "Expected an UINT, not " + t.getType() );
					return new Date( t.getValue() );
				}
				else if( t.getValue() == 0x100 )
					this.namespace = new ArrayList<Object>();
				return read(); // Ignore rest of the tags

			case MAP:
				checkState();
				pushState( STATE.ARRAYMAP );
				int len = t.getLength();
				JSONObject object = new JSONObject();
				for( int i = 0; i < len; i++ )
					object.set( (String)readNoStream(), readNoStream() ); // TODO Throw better exception for the cast?
				popState();
				return object;

			case ARRAY:
				checkState();
				pushState( STATE.ARRAYMAP );
				len = t.getLength();
				JSONArray array = new JSONArray();
				for( int i = 0; i < len; i++ )
					array.add( readNoStream() );
				popState();
				return array;

			case IARRAY:
				checkState();
				pushState( STATE.ARRAYMAP );
				array = new JSONArray();
				for( Object o = readNoStream(); o != null; o = readNoStream() )
					array.add( o );
//				popState();
				return array;

			case BSTRING:
				if( this.state == STATE.ITEXT )
					throw new IllegalStateException( "Only text strings allowed" );
				len = t.getLength();
				byte[] bytes = new byte[ len ];
				in.readBytes( bytes );
				if( this.namespace != null )
				{
					int index = this.namespace.size();
					if( len >= CBORWriter.getUIntSize( index ) + 2 )
						this.namespace.add( bytes );
				}
				return bytes;

			case TSTRING:
				if( this.state == STATE.IBYTES )
					throw new IllegalStateException( "Only byte strings allowed" );
				len = t.getLength();
				String s = in.readString( len );
				if( this.namespace != null )
				{
					int index = this.namespace.size();
					if( len >= CBORWriter.getUIntSize( index ) + 2 )
						this.namespace.add( s );
				}
				return s;

			case ITSTRING:
			{
				checkState();
				pushState( STATE.ITEXT );
				Reader result = new InputStreamReader( this.disk.buffer( new CBORBytesInputStream( in ) ), Charset.forName( "UTF-8" ) );
				popState();
				return result;
			}

			case IBSTRING:
				checkState();
				pushState( STATE.IBYTES );
				InputStream result = this.disk.buffer( new CBORBytesInputStream( in ) );
				popState();
				return result;

			case UINT:
				checkState();
				return t.getValue();

			case DFLOAT:
				checkState();
				return t.getDouble();

			case BOOL:
				checkState();
				return t.getBoolean();

			case BREAK:
				popState();
				//$FALL-THROUGH$
			case EOF:
				return null;

			default:
				throw new UnsupportedOperationException( type.toString() );
		}
	}

	public Object readNoStream()
	{
		Object result = read();
		if( result instanceof Reader || result instanceof InputStream )
			throw new UnsupportedOperationException( result.getClass().getName() );
		return result;
	}

	private void pushState( STATE state )
	{
		this.states.push( new StateItem( this.state, this.namespace ) );
		this.state = state;
	}

	private void popState()
	{
		StateItem state = this.states.pop();
		this.state = state.state;
		this.namespace = state.namespace;
	}

	private void checkState()
	{
		if( this.state == STATE.IBYTES )
			throw new IllegalStateException( "Only byte strings allowed" );
		if( this.state == STATE.ITEXT )
			throw new IllegalStateException( "Only text strings allowed" );
	}

	static private class StateItem
	{
		STATE state;
		List<Object> namespace;

		StateItem( STATE state, List<Object> namespace )
		{
			this.state = state;
			this.namespace = namespace;
		}
	}
}
