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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import solidbase.core.Assert;
import solidbase.core.SystemException;


/**
 * Reads an element from an {@link XMLStreamReader}.
 * 
 * @author René M. de Bloois
 */
public class StaxNodeReader
{
	/**
	 * This utility class cannot be constructed.
	 */
	private StaxNodeReader()
	{
		super();
	}

	/**
	 * Reads a element from the given {@link XMLStreamReader}.
	 * 
	 * @param reader The {@link XMLStreamReader} to read a node from.
	 * @return The element that is read.
	 * @throws XMLStreamException When thrown by the {@link XMLStreamReader}.
	 */
	static public Element readNode( XMLStreamReader reader ) throws XMLStreamException
	{
		int event = reader.getEventType();
		Assert.isTrue( event == XMLStreamConstants.START_ELEMENT, "Expecting an element start" );

		Element element = new Element( reader.getLocalName(), null );
		read( reader, element );
		return element;
	}

	/**
	 * A recursive method to read elements from the {@link XMLStreamReader}.
	 * 
	 * @param reader The {@link XMLStreamReader} to read a node from.
	 * @param element The element to populate with attributes and children.
	 * @throws XMLStreamException When thrown by the {@link XMLStreamReader}.
	 */
	static protected void read( XMLStreamReader reader, Element element ) throws XMLStreamException
	{
		for( int i = 0; i < reader.getAttributeCount(); i++ )
			element.addAttribute( reader.getAttributeLocalName( i ), reader.getAttributeValue( i ) );

		StringBuilder buffer = new StringBuilder();

		while( reader.hasNext() )
		{
			int event = reader.next();
			if( event == XMLStreamConstants.CHARACTERS )
			{
				if( !reader.isWhiteSpace() )
					buffer.append( reader.getText() );
			}
			else if( event == XMLStreamConstants.START_ELEMENT )
			{
				if( buffer.length() > 0 )
				{
					element.addText( buffer.toString() );
					buffer.setLength( 0 );
				}

				Element child = new Element( reader.getLocalName(), element );
				element.addChild( child );

				read( reader, child );
			}
			else if( event == XMLStreamConstants.END_ELEMENT )
			{
				if( buffer.length() > 0 )
				{
					element.addText( buffer.toString() );
					buffer.setLength( 0 );
				}
				return;
			}
			else if( event == XMLStreamConstants.COMMENT )
			{
				// Ignore comments
			}
			else
				throw new SystemException( "Unexpected event [" + event + "]" );
		}
	}
}
