package solidstack.cbor;

import java.util.ArrayList;
import java.util.List;


public class StandardReverseByteStringIndex implements ReverseByteStringIndex
{
	private List<ByteString> list = new ArrayList<ByteString>();


	@Override
	public void put( ByteString value )
	{
		int index = this.list.size();
		if( value.length() >= CBORWriter.getUIntSize( index ) + 2 )
			this.list.add( value );
	}

	@Override
	public ByteString get( int index )
	{
		return this.list.get( index );
	}
}
