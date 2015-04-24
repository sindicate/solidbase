package solidbase.http;

import java.util.HashMap;
import java.util.Map;

public class Parameters
{
	protected Map< String, Object > params = new HashMap< String, Object >();

	public Parameters( Parameters parent )
	{
		if( parent != null )
			this.params.putAll( parent.params );
	}

	public Parameters put( String name, Object value )
	{
		this.params.put( name, value );
		return this;
	}

	public Object get( String name )
	{
		return this.params.get( name );
	}
}
