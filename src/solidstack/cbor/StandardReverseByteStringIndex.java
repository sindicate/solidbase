package solidstack.cbor;

import java.util.ArrayList;
import java.util.List;


public class StandardReverseByteStringIndex implements ReverseByteStringIndex
{
	private List<CBORByteString> list = new ArrayList<CBORByteString>();


	@Override
	public void put( CBORByteString value )
	{
		int index = this.list.size();
		if( value.length() >= CBORWriter.getUIntSize( index ) + 2 )
			this.list.add( value );
	}

	@Override
	public CBORByteString get( int index )
	{
		return this.list.get( index );
	}
}
