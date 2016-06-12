package solidstack.cbor;

import java.util.HashMap;
import java.util.Map;


public class SlidingReverseByteStringIndex implements ReverseByteStringIndex
{
	private Map<Integer, CBORByteString> map = new HashMap<Integer, CBORByteString>();

	private int capacity;
	private int maxItemSize;
	private int nextIndex;


	public SlidingReverseByteStringIndex( int capacity, int maxItemSize )
	{
		this.capacity = capacity;
		this.maxItemSize = maxItemSize;
		this.nextIndex = capacity - 1;
	}

	public void put( CBORByteString value )
	{
		if( value.length() > this.maxItemSize )
			return;
		int index = this.nextIndex;
		this.map.put( index, value );
		this.nextIndex = wrap( index - 1 );
	}

	public CBORByteString get( int index )
	{
		CBORByteString result = get0( index );
		put( result ); // New occurrence
		return result;
	}

	CBORByteString get0( int index )
	{
		if( index < 0 || index >= this.capacity )
			throw new IndexOutOfBoundsException( "index: " + index + ", capacity: " + this.capacity );
		index = cap( index + this.nextIndex + 1 );
		return this.map.get( index );
	}

	private int wrap( int value )
	{
		if( value < 0 )
			return value + this.capacity;
		return value;
	}

	private int cap( int value )
	{
		if( value >= this.capacity )
			return value - this.capacity;
		return value;
	}
}
