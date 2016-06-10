package solidstack.cbor;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;

import solidstack.cbor.CBORToken.TYPE;
import solidstack.io.SourceInputStream;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;


public class CBORReader
{
	static private enum STATE { IBYTES, ITEXT, ARRAYMAP };

	private CBORParser in;

	// TODO Lazy init
	private DiskBuffer disk = new DiskBuffer( "cborbuffer" );


	public CBORReader( SourceInputStream in )
	{
		this.in = new CBORParser( in );
	}

	public void close()
	{
		this.in.close();
		this.disk.close();
	}

	public CBORToken get()
	{
		return this.in.get();
	}

	public Object read()
	{
		CBORParser in = this.in;
		long pos = in.getPos();
		CBORToken t = in.get();
		TYPE type = t.type();
		switch( type )
		{
			case MAP:
				int len = t.length();
				JSONObject object = new JSONObject();
				for( int i = 0; i < len; i++ )
					object.set( (String)read(), read() ); // TODO Throw better exception for the cast?
				return object;

			case ARRAY:
				len = t.length();
				JSONArray array = new JSONArray();
				for( int i = 0; i < len; i++ )
					array.add( read() );
				return array;

			case IARRAY:
				array = new JSONArray();
				for( Object o = read(); o != null; o = read() )
					array.add( o );
				return array;

			case BYTES:
				byte[] bytes = new byte[ t.length() ];
				this.in.readBytes( bytes );
				return bytes;

			case TEXT:
				len = t.length();
				return this.in.readString( len );

			case ITEXT:
				// TODO If only 1 TSTRING then return normal String
				return new InputStreamReader( this.disk.buffer( new CBORBytesInputStream( this.in ) ), Charset.forName( "UTF-8" ) );

			case IBYTES:
				// TODO If only 1 BSTRING then return normal byte[]
				return this.disk.buffer( new CBORBytesInputStream( this.in ) );

			case UINT:
				if( t.hasTag( 0x19 ) )
					return this.in.getFromNamespace( t.length() );
				if( t.hasTag( 0x01 ) )
					return new Date( t.longValue() );
				return t.longValue();

			case DFLOAT:
				return t.doubleValue();

			case BOOL:
				return t.booleanValue();

			case NULL:
			case BREAK:
			case EOF:
				return null;

			default:
				throw new UnsupportedOperationException( "Unexpected token: " + t );
		}
	}
}
