package solidstack.cbor;

import java.util.HashMap;
import java.util.Map;


public class StandardByteStringIndex implements ByteStringIndex
{
	/* *******
	 * 32-bit JVM or 64-bit with UseCompressedOops=true (default)
	 *
	 * dictionary = HashMap
	 * 		table
	 *		4+8		--> HashMap$Node
	 *		4			hash
	 *		4+8			--> key = ByteString
	 *		4+8				--> byte[]
	 *		1				boolean
	 *		4			--> next
	 *		4+8			--> value = Integer
	 *		4				value
	 *		3	padding
	 */
	static public int MEMORY_OVERHEAD = 64;

	private Map<ByteString, Integer> map = new HashMap<ByteString, Integer>();
	private int memoryUsage;


	void put( ByteString value )
	{
		int index = this.map.size();
		if( value.length() >= CBORWriter.getUIntSize( index ) + 2 )
		{
			this.map.put( value, index );
			this.memoryUsage += value.length() + MEMORY_OVERHEAD;
		}
	}

	public Integer putOrGet( ByteString value )
	{
		Integer result = get( value );
		if( result != null )
			return result;
		put( value );
		return null;
	}

	Integer get( ByteString value )
	{
		return this.map.get( value );
	}

	public int memoryUsage()
	{
		return this.memoryUsage;
	}
}
