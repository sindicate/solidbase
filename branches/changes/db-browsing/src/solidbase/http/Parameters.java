package solidbase.http;

import java.util.HashMap;
import java.util.Map;

public class Parameters
{
	static public final Parameters NONE = new Parameters();

	protected Map< String, Object > params = new HashMap< String, Object >();

	public Parameters put( String name, Object value )
	{
		if( this == NONE )
			return new Parameters().put( name, value );
		this.params.put( name, value );
		return this;
	}

	public Object get( String name )
	{
		return this.params.get( name );
	}
}
