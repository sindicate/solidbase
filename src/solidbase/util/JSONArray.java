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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import solidbase.core.SystemException;

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

	public BigDecimal findNumber( int index )
	{
		Object result = this.values.get( index );
		if( result == null || result instanceof BigDecimal )
			return (BigDecimal)result;
		throw new SystemException( "Value at index ' + index + ' is not a BigDecimal" );
	}

	public BigDecimal getNumber( int index )
	{
		BigDecimal result = findNumber( index );
		if( result == null )
			throw new SystemException( "Missing value at index " + index );
		return result;
	}

	public String findString( int index )
	{
		Object result = this.values.get( index );
		if( result == null || result instanceof String )
			return (String)result;
		throw new SystemException( "Value at index ' + index + ' is not a String" );
	}

	public String getString( int index )
	{
		String result = findString( index );
		if( result == null )
			throw new SystemException( "Missing value at index " + index );
		return result;
	}

	public JSONArray findArray( int index )
	{
		Object result = this.values.get( index );
		if( result == null || result instanceof JSONArray )
			return (JSONArray)result;
		throw new SystemException( "Value at index ' + index + ' is not a JSONArray" );
	}

	public JSONArray getArray( int index )
	{
		JSONArray result = findArray( index );
		if( result == null )
			throw new SystemException( "Missing value at index " + index );
		return result;
	}

	public JSONObject findObject( int index )
	{
		Object result = this.values.get( index );
		if( result == null || result instanceof JSONObject )
			return (JSONObject)result;
		throw new SystemException( "Value at index ' + index + ' is not a JSONObject" );
	}

	public JSONObject getObject( int index )
	{
		JSONObject result = findObject( index );
		if( result == null )
			throw new SystemException( "Missing value at index " + index );
		return result;
	}

	public Object set( int index, Object value )
	{
		return this.values.set( index, value );
	}
}
