package solidstack.cbor;

import java.util.HashMap;
import java.util.Map;

import solidstack.cbor.TreeIndex.Node;

public class SlidingReverseByteStringIndex implements ReverseByteStringIndex
{
	private Map<CBORByteString, Node<CBORByteString>> map = new HashMap<CBORByteString, Node<CBORByteString>>();
	private TreeIndex<CBORByteString> index = new TreeIndex<CBORByteString>();

	private int capacity;
	private int maxItemSize;


	public SlidingReverseByteStringIndex( int capacity, int maxItemSize )
	{
		this.capacity = capacity;
		this.maxItemSize = maxItemSize;
	}

	public void put( CBORByteString value )
	{
		if( value.length() < 2 )
			return;
		if( value.length() > this.maxItemSize )
			return;

		Node<CBORByteString> node = this.map.remove( value );
		if( node != null )
			this.index.remove( node );
		else if( this.index.size() >= this.capacity )
			this.map.remove( this.index.removeLast().data );

		this.map.put( value, this.index.addFirst( value ) );
	}

	public CBORByteString get( int index )
	{
		CBORByteString result = get0( index );
		put( result ); // New occurrence
		return result;
	}

	CBORByteString get0( int index )
	{
		return this.index.get( index ).data;
	}
}
