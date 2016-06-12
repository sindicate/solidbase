package solidstack.cbor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class SlidingByteStringIndex implements ByteStringIndex
{
	private Map<CBORByteString, Integer> map = new HashMap<CBORByteString, Integer>();
	private LinkedList<Object[]> window = new LinkedList<Object[]>();

	private int capacity;
	private int maxItemSize;
	private int nextIndex;


	public SlidingByteStringIndex( int capacity, int maxItemSize )
	{
		this.capacity = capacity;
		this.maxItemSize = maxItemSize;
		this.nextIndex = capacity - 1;
	}

	void put( CBORByteString value )
	{
		if( this.window.size() >= this.capacity )
		{
			Object[] item = this.window.removeFirst();
			if( this.map.get( item[ 0 ] ).equals( item[ 1 ] ) ) // TODO Java 8 has a remove(key,value)
				this.map.remove( item[ 0 ] );
		}

		int index = this.nextIndex;
		this.map.put( value, index );
		this.nextIndex = wrap( index - 1 );

		this.window.addLast( new Object[] { value, index } );
	}

	public Integer putOrGet( CBORByteString value )
	{
		if( value.length() < 3 )
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
		Integer index = this.map.get( value );
		if( index == null )
			return null;
		return wrap( index - ( this.nextIndex + 1 ) );
	}

	public int memoryUsage()
	{
		return 0;
	}

	private int wrap( int value )
	{
		if( value < 0 )
			return value + this.capacity;
		return value;
	}
}
