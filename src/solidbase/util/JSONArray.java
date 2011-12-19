package solidbase.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JSONArray implements Iterable< Object >
{
	protected List< Object > values = new ArrayList< Object >();

	public void add( Object value )
	{
		this.values.add( value );
	}

	public Iterator< Object > iterator()
	{
		return this.values.iterator();
	}
}
