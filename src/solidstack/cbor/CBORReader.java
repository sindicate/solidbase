package solidstack.cbor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;


public class CBORReader
{
	private CBORScanner in;

	private List<Object> namespace;


	public CBORReader( InputStream in )
	{
		this.in = new CBORScanner( in );
	}

	public CBORScanner getCBORScanner()
	{
		return this.in;
	}

	public Object read()
	{
		CBORScanner in = this.in;
		Token t = in.get();
		TYPE type = t.getType();
		switch( type )
		{
			case TAG:
				if( t.getValue() == 0x100 )
					this.namespace = new ArrayList<Object>();
				else if( t.getValue() == 0x19 )
				{
					t = in.get();
					if( t.getType() != TYPE.UINT )
						throw new CBORException( "Expected an UINT, not " + t.getType() );
					return this.namespace.get( t.getLength() );
				}
				return read(); // Ignore rest of the tags

			case MAP:
				int len = t.getLength();
				JSONObject object = new JSONObject();
				for( int i = 0; i < len; i++ )
					object.set( (String)read(), read() ); // TODO Throw better exception for the cast?
				return object;

			case ARRAY:
				len = t.getLength();
				JSONArray array = new JSONArray();
				for( int i = 0; i < len; i++ )
					array.add( read() );
				return array;

			case TSTRING:
				len = t.getLength();
				String s = in.readString( len );
				if( this.namespace != null )
				{
					int index = this.namespace.size();
					if( len >= CBORWriter.getUIntSize( index ) + 2 )
						this.namespace.add( s );
				}
				return s;

			case UINT:
				return t.getValue();

			default:
				throw new UnsupportedOperationException( type.toString() );
		}
	}
}
