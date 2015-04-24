package solidbase.http;

import java.util.Map;
import java.util.WeakHashMap;

public class DebugId
{
	static protected int id = 1;
	static protected Map< Object, Integer > idMap = new WeakHashMap< Object, Integer >();

	static public int getId( Object object )
	{
		Integer id2 = idMap.get( object );
		if( id2 != null )
			return id2;

		int id3 = id++;
		idMap.put( object, id3 );
		return id3;
	}
}
