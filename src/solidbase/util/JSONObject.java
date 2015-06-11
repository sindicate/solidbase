/*--
 * Copyright 2010 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.util;

import java.math.BigDecimal;
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

	public BigDecimal findNumber( String name )
	{
		Object result = this.values.get( name );
		if( result == null || result instanceof BigDecimal )
			return (BigDecimal)result;
		throw new SystemException( "Attribute '" + name + "' is not a BigDecimal" );
	}

	public BigDecimal getNumber( String name )
	{
		BigDecimal result = findNumber( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		return result;
	}

	public String findString( String name )
	{
		Object result = this.values.get( name );
		if( result == null || result instanceof String )
			return (String)result;
		throw new SystemException( "Attribute '" + name + "' is not a String" );
	}

	public String getString( String name )
	{
		String result = findString( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		return result;
	}

	public JSONArray findArray( String name )
	{
		Object result = this.values.get( name );
		if( result == null || result instanceof JSONArray )
			return (JSONArray)result;
		throw new SystemException( "Attribute '" + name + "' is not an JSONArray" );
	}

	public JSONArray getArray( String name )
	{
		JSONArray result = findArray( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		return result;
	}

	public JSONObject findObject( String name )
	{
		Object result = this.values.get( name );
		if( result == null || result instanceof JSONObject )
			return (JSONObject)result;
		throw new SystemException( "Attribute '" + name + "' is not an JSONObject" );
	}

	public JSONObject getObject( String name )
	{
		JSONObject result = findObject( name );
		if( result == null )
			throw new SystemException( "Missing attribute '" + name + "'" );
		return result;
	}

	public Iterator< Map.Entry< String, Object >> iterator()
	{
		return this.values.entrySet().iterator();
	}
}
