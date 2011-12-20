package solidbase.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import solidbase.core.SystemException;

public class JSONObject implements Iterable< Map.Entry< String, Object > >
{
	protected Map< String, Object > values = new LinkedHashMap< String, Object >();

	public JSONObject( Object... values )
	{
		int i = 0;
		while( i < values.length )
		{
			if( !( values[ i ] instanceof String ) )
				throw new IllegalArgumentException( "Arg " + ( i + 1 ) + " is not a String" );
			set( (String)values[ i++ ], values[ i++ ] );
		}
	}

	public void set( String name, Object value )
	{
		this.values.put( name, value );
	}

	public String getString( String name )
	{
		Object result = this.values.get( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		if( result instanceof String )
			return (String)result;
		throw new SystemException( "Attribute '" + name + "' is not a String" );
	}

	public JSONArray getArray( String name )
	{
		Object result = this.values.get( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		if( result instanceof JSONArray )
			return (JSONArray)result;
		throw new SystemException( "Attribute '" + name + "' is not an JSONArray" );
	}

	public Iterator< Map.Entry< String, Object >> iterator()
	{
		return this.values.entrySet().iterator();
	}
}
