package solidstack.cbor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class SlidingByteStringIndex implements ByteStringIndex
{
	private Map<CBORByteString, Integer> map = new HashMap<CBORByteString, Integer>();
	private LinkedList<CBORByteString> window = new LinkedList<CBORByteString>();

	private int capacity;
	private int nextIndex;


	public SlidingByteStringIndex( int capacity )
	{
		this.capacity = capacity;
		this.nextIndex = capacity - 1;
	}

	void put( CBORByteString value )
	{
		if( this.window.size() >= this.capacity )
			this.map.remove( this.window.removeFirst() );

		int index = this.nextIndex;
		this.map.put( value, index );
		this.nextIndex = wrap( index - 1 );

		this.window.addLast( value );
	}

	public Integer putOrGet( CBORByteString value )
	{
		Integer result = get( value );
		put( value ); // New occurrence
		return result;
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
