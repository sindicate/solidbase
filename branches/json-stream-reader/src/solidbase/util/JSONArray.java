package solidbase.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class JSONArray implements Iterable< Object >
{
	protected List< Object > values = new ArrayList< Object >();

	public void add( Object value )
	{
		this.values.add( value );
	}

	public ListIterator< Object > iterator()
	{
		return this.values.listIterator();
	}

	public int size()
	{
		return this.values.size();
	}

	public Object get( int index )
	{
		return this.values.get( index );
	}
}
