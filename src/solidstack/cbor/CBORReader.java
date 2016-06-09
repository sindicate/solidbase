package solidstack.cbor;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;

import solidstack.cbor.CBORScanner.Token;
import solidstack.cbor.CBORScanner.Token.TYPE;
import solidstack.io.SourceException;
import solidstack.io.SourceInputStream;
import solidstack.io.SourceLocation;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;


public class CBORReader
{
	static private enum STATE { IBYTES, ITEXT, ARRAYMAP };

	private CBORParser in;

	private DiskBuffer disk = new DiskBuffer( "cborbuffer" );


	public CBORReader( SourceInputStream in )
	{
		this.in = new CBORParser( in );
	}

//	public CBORScanner getCBORScanner()
//	{
//		return this.in;
//	}

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
		CBORParser in = this.in;
		long pos = in.getPos();
		Token t = in.get();
		TYPE type = t.type();
		switch( type )
		{
			case TAG:
				if( t.longValue() == 0x19 )
				{
					t = in.get();
					if( t.type() != TYPE.UINT )
						throw new SourceException( "Expected a UINT, not " + t, SourceLocation.forBinary( in.getResource(), pos ) );
					return this.in.getFromNamespace( t.length() );
				}
				if( t.longValue() == 0x01 )
				{
					t = in.get();
					if( t.type() != TYPE.UINT )
						throw new SourceException( "Expected a UINT, not " + t, SourceLocation.forBinary( in.getResource(), pos ) );
					return new Date( t.longValue() );
				}
				if( t.longValue() == 0x100 )
					return read();
				throw new UnsupportedOperationException( "Unsupported token: " + t );

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
				return t.longValue();

			case DFLOAT:
				return t.doubleValue();

			case BOOL:
				return t.booleanValue();

			case BREAK:
			case EOF:
				return null;

			default:
				throw new UnsupportedOperationException( "Unsupported token: " + t );
		}
	}
}
