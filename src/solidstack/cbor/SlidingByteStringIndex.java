package solidstack.cbor;

import java.util.HashMap;
import java.util.Map;

import solidstack.cbor.TreeIndex.Node;


public class SlidingByteStringIndex implements ByteStringIndex
{
	private Map<ByteString, Node<ByteString>> map = new HashMap<ByteString, Node<ByteString>>();
	private TreeIndex<ByteString> index = new TreeIndex<ByteString>();

	private int capacity;
	private int maxItemSize;


	public SlidingByteStringIndex( int capacity, int maxItemSize )
	{
		this.capacity = capacity;
		this.maxItemSize = maxItemSize;
	}

	void put( ByteString value )
	{
		Node<ByteString> node = this.map.remove( value );
		if( node != null )
			this.index.remove( node );
		else if( this.index.size() >= this.capacity )
			this.map.remove( this.index.removeLast().data );

		this.map.put( value, this.index.addFirst( value ) );
	}

	@Override
	public Integer putOrGet( ByteString value )
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

	Integer get( ByteString value )
	{
		Node<ByteString> node = this.map.get( value );
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
