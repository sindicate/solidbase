package solidstack.cbor;

import java.util.HashMap;
import java.util.Map;

import solidstack.cbor.TreeIndex.Node;


public class SlidingByteStringIndex implements ByteStringIndex
{
	private Map<CBORByteString, Node<CBORByteString>> map = new HashMap<CBORByteString, Node<CBORByteString>>();
	private TreeIndex<CBORByteString> index = new TreeIndex<CBORByteString>();

	private int capacity;
	private int maxItemSize;


	public SlidingByteStringIndex( int capacity, int maxItemSize )
	{
		this.capacity = capacity;
		this.maxItemSize = maxItemSize;
	}

	void put( CBORByteString value )
	{
		Node<CBORByteString> node = this.map.remove( value );
		if( node != null )
			this.index.remove( node );
		else if( this.index.size() >= this.capacity )
			this.map.remove( this.index.removeLast().data );

		this.map.put( value, this.index.addFirst( value ) );
	}

	@Override
	public Integer putOrGet( CBORByteString value )
	{
		if( value.length() < 2 )
			return null;
		if( value.length() > this.maxItemSize )
			return null;
		Integer result = get( value );
		put( value ); // New occurrence
		if( result == null )
			return null;
		if( value.length() >= CBORWriter.getUIntSize( result ) + 2 )
			return result;
		return null;
	}

	Integer get( CBORByteString value )
	{
		Node<CBORByteString> node = this.map.get( value );
		if( node == null )
			return null;
		return node.index();
	}

	@Override
	public int memoryUsage()
	{
		return 0;
	}
}
