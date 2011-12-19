package solidbase.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject implements Iterable< Map.Entry< String, Object > >
{
	protected Map< String, Object > values = new LinkedHashMap< String, Object >();

	public void set( String name, Object value )
	{
		this.values.put( name, value );
	}

	public Iterator< Map.Entry< String, Object >> iterator()
	{
		return this.values.entrySet().iterator();
	}
}
