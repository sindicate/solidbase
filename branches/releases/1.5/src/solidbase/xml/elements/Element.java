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

package solidbase.xml.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.core.SystemException;

/**
 * 
 * @author R.M. de Bloois
 */
public class Element
{
	protected String name;
	protected Element parent;
	protected List< Object > children;
	protected Map< String, String > attributes;

	public Element( String name, Element parent )
	{
		this.name = name;
		this.parent = parent;
	}

	public String getName()
	{
		return this.name;
	}

	public List< Object > getChildren()
	{
		if( this.children == null )
			return Collections.EMPTY_LIST;
		return this.children;
	}

	public String getText()
	{
		if( this.children == null )
			return "";
		if( this.children.size() != 1 )
			throw new SystemException( "Expecting only 1 text node" );
		Object object = this.children.get( 0 );
		if( object instanceof String )
			return (String)object;
		throw new SystemException( "Expecting a text node" );
	}

	public Element findElement( String name )
	{
		if( this.children == null )
			return null;

		Element result = null;

		for( Object object : this.children )
			if( object instanceof Element )
				if( ( (Element)object ).name.equals( name ) )
				{
					if( result != null )
						throw new SystemException( "Found more than 1 element with name \"" + name + "\"" );
					result = (Element)object;
				}

		return result;
	}

	public List< Element > findElements( String name )
	{
		if( this.children == null )
			return Collections.EMPTY_LIST;

		List< Element > result = new ArrayList< Element >();

		for( Object object : this.children )
			if( object instanceof Element )
				if( ( (Element)object ).name.equals( name ) )
					result.add( (Element)object );

		return result;
	}

	public void addChild( Element child )
	{
		if( this.children == null )
			this.children = new ArrayList< Object >();
		this.children.add( child );
	}

	public void addText( String text )
	{
		if( this.children == null )
			this.children = new ArrayList< Object >();
		this.children.add( text );
	}

	public void addAttribute( String name, String value )
	{
		if( this.attributes == null )
			this.attributes = new HashMap< String, String >();
		this.attributes.put( name, value );
	}
}
