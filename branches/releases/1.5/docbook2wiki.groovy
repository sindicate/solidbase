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

import javax.xml.stream.*
import solidbase.xml.elements.*

def reader = XMLInputFactory.newInstance().createXMLStreamReader( new InputStreamReader( new FileInputStream( "doc/manual.xml" ), "UTF-8" ) )
assert reader.hasNext()
reader.next()
assert reader.hasNext()
reader.next()

def book = StaxNodeReader.readNode( reader )
assert book.name == "book"

new File( "../solidbase-wiki/UsersManual.wiki" ).withPrintWriter
{
	def info = book.findElement("info")
	assert info
	it.println "=${info.findElement("title").text}="
	it.println "==${info.findElement("subtitle").text}=="
	it.println "===${info.findElement("author").findElement("personname").text}==="
	it.println "----"
	
	def chapters = book.children
	for( chapter in chapters )
	{
		if( chapter.name == "chapter" )
		{
			it.println "=${chapter.findElement("title").text}="
			chapterorsection( it, chapter, 0 )
		}
		else
			assert chapter.name == "info" : "Unexpected child of book: " + chapter.name
	}
}

def chapterorsection( out, node, int sectiondepth )
{
	for( child in node.children )
	{
		if( child.name == "section" )
		{
			out.println "==${titleToWiki(child.findElement("title"))}=="
			chapterorsection( out, child, sectiondepth + 1 )
		}
		else if( child.name == "para" )
		{
			para( out, child )
		}
		else if( child.name == "itemizedlist" )
		{
			out.println "itemizedlist"
		}
		else if( child.name == "example" )
		{
			out.println "example"
		}
		else if( child.name == "screen" )
		{
			out.println "screen"
		}
		else if( child.name == "table" )
		{
			out.println "table"
		}
		else if( child.name == "section" )
		{
			out.println "section"
		}
		else if( child.name == "programlisting" )
		{
			out.println "programlisting"
		}
		else if( child.name == "note" )
		{
			out.println "note"
		}
		else
			assert child.name == "title" : "Unexpected child ${child.name} of parent ${node.name}"
	}
}

def para( out, element )
{
	for( child in element.children )
	{
		if( child instanceof String )
			out.print child
		else
		{
			assert child instanceof Element
			if( child.name == "code" || child.name == "programlisting" || child.name == "computeroutput" )
			{
				out.print "{{{${child.text}}}}"
			}
			else if( child.name == "xref" )
				out.print "TODO: XREF"
			else
				assert false : "got ${child.name}"
		}
	}
	out.println()
}

def titleToWiki( def element )
{
	def result = new StringBuilder()
	for( child in element.children )
	{
		if( child instanceof String )
			result << child
		else
		{
			assert child instanceof Element
			assert child.name == "code"
			result << "{{{"
			result << child.text
			result << "}}}"
		}
	}
	return result
}
