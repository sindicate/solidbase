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
 * Represents an element with child elements and attributes. Can be used to read xml data.
 * 
 * @author René M. de Bloois
 */
public class Element
{
	/**
	 * The name of the element.
	 */
	protected String name;

	/**
	 * The parent of this element. Can be null for the top element.
	 */
	protected Element parent;

	/**
	 * The children of this element. Will be null if there are no children.
	 */
	protected List< Object > children;

	/**
	 * The attributes of this element. Will be null if there are no attributes.
	 */
	protected Map< String, String > attributes;

	/**
	 * Constructs a new element with the given name and parent.
	 * 
	 * @param name The name of the element.
	 * @param parent The parent of the element. May be null.
	 */
	public Element( String name, Element parent )
	{
		this.name = name;
		this.parent = parent;
	}

	/**
	 * Returns the name of the element.
	 * @return The name of the element.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the children of the element.
	 * 
	 * @return The children of the element. An empty list when there are no children.
	 */
	public List< Object > getChildren()
	{
		if( this.children == null )
			return Collections.emptyList();
		return this.children;
	}

	/**
	 * Returns the text contained in the element. The element must not have other child elements.
	 * 
	 * @return The text contained in the element.
	 */
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

	/**
	 * Finds a child element with the given name. A {@link SystemException} is thrown when more than 1 element is found.
	 * 
	 * @param name The name of the child element to find.
	 * @return The child element with the given name. Null when a child with that name does not exist.
	 */
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

	/**
	 * Finds all the child elements with the given name.
	 * 
	 * @param name The name of the child elements to find.
	 * @return A list of child elements with the given name. An empty list when children with that name do not exist.
	 */
	public List< Element > findElements( String name )
	{
		if( this.children == null )
			return Collections.emptyList();

		List< Element > result = new ArrayList< Element >();

		for( Object object : this.children )
			if( object instanceof Element )
				if( ( (Element)object ).name.equals( name ) )
					result.add( (Element)object );

		return result;
	}

	/**
	 * Find the attribute with the given name.
	 * 
	 * @param name The name of the attribute to find.
	 * @return The attribute with the given name. Null if the attribute does not exist.
	 */
	public String findAttribute( String name )
	{
		return this.attributes.get( name );
	}

	/**
	 * Add the given child to the element.
	 * 
	 * @param child The child element to add.
	 */
	public void addChild( Element child )
	{
		if( this.children == null )
			this.children = new ArrayList< Object >();
		this.children.add( child );
	}

	/**
	 * Add a text child to the element.
	 * 
	 * @param text The text child to add.
	 */
	public void addText( String text )
	{
		if( this.children == null )
			this.children = new ArrayList< Object >();
		this.children.add( text );
	}

	/**
	 * Add an attribute to the element.
	 * 
	 * @param name The name of the attribute to add.
	 * @param value The value of the attribute to add.
	 */
	public void addAttribute( String name, String value )
	{
		if( this.attributes == null )
			this.attributes = new HashMap< String, String >();
		this.attributes.put( name, value );
	}
}
