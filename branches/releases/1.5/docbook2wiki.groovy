/*--
 * Copyright 2010 Ren� M. de Bloois
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

try
{
	def reader = XMLInputFactory.newInstance().createXMLStreamReader( new InputStreamReader( new FileInputStream( "doc/manual.xml" ), "UTF-8" ) )
	assert reader.hasNext()
	reader.next()
	assert reader.hasNext()
	reader.next()
	
	def book = StaxNodeReader.readNode( reader )
	assert book.name == "book"
	
	new File( "../solidbase-wiki/UsersManual.wiki" ).withPrintWriter( "UTF-8" )
	{
		def out = new WikiWriter( it )
		
		def info = book.findElement("info")
		assert info
		
		out.text( "Generated from DocBook\n" )
		out.text( "=" + info.findElement("title").text + "=\n" )
		out.text( "==" + info.findElement("subtitle").text + "==\n" )
		out.text( "===" + info.findElement("author").findElement("personname").text + "===\n" )
		out.text( "----" )
		
		for( chapter in book.children )
		{
			if( chapter.name == "chapter" )
				section( out, chapter )
			else
				assert chapter.name == "info" : "Got ${chapter.name}"
		}
		
		out.newline()
	}
}
catch( Throwable e )
{
	throw org.codehaus.groovy.runtime.StackTraceUtils.deepSanitize( e )
}

def dotitle( out, title )
{
	for( child in title.children )
	{
		if( child instanceof String )
			out.text( child )
		else if( child.name == "code" )
		{
			out.startCode()
			out.text( child.text )
			out.endCode()
		}
		else
			assert false : "Got ${child.name}"
	}
}

def section( out, section )
{
	out.startSection()
	def title = section.findElement("title")
	assert title
	out.startHeader()
	dotitle( out, title )
	out.endHeader()
	for( child in section.children )
	{
		if( child.name == "section" )
			section( out, child )
		else if( child.name == "para" )
			para( out, child )
		else if( child.name == "itemizedlist" )
			itemizedlist( out, child )
		else if( child.name == "example" )
			example( out, child )
		else if( child.name == "screen" )
			codeblock( out, child )
		else if( child.name == "table" )
			table( out, child )
		else if( child.name == "programlisting" )
			codeblock( out, child )
		else if( child.name == "note" )
			note( out, child )
		else
			assert child.name == "title" : "Got ${child.name}"
	}
	out.endSection()
}

def container( out, section )
{
	for( child in section.children )
	{
		if( child.name == "para" )
			para( out, child )
//		else if( child.name == "itemizedlist" )
//			itemizedlist( out, child )
		else if( child.name == "programlisting" )
			code( out, child )
		else
			assert false : "Got ${child.name}"
	}
}

def para( out, section )
{
	out.startPara()
	for( child in section.children )
	{
		if( child instanceof String )
			out.text( child )
		else if( child.name == "programlisting" )
			codeblock( out, child )
		else if( child.name == "xref" )
			out.todo( child.name )
		else if( child.name == "note" )
			note( out, child )
		else if( child.name == "code" || child.name == "computeroutput" )
			code( out, child )
		else
			assert false : "Got ${child.name}"
	}
	out.endPara()
}

def example( out, example )
{
	def title = example.findElement( "title" )
	assert title
	out.startObjectHeader()
	dotitle( out, title )
	out.endObjectHeader()
	out.startSection()
	for( child in example.children )
	{
		if( child.name == "screen" || child.name == "programlisting" )
			codeblock( out, child )
		else if( child.name == "para" )
			para( out, child )
		else
			assert child.name == "title" : "Got ${child.name}"
	}
	out.endSection()
}

def itemizedlist( out, section )
{
	def title = section.findElement( "title" )
	if( title )
	{
		out.startObjectHeader()
		dotitle( out, title )
		out.endObjectHeader()
	}
	out.startItemizedList()
	for( child in section.children )
	{
		if( child.name == "listitem" )
		{
			out.startItem()
			container( out, child )
			out.endItem()
		}
		else
			assert child.name == "title" : "Got ${child.name}"
	}
	out.endItemizedList()
}

def code( out, code )
{
	out.startCode()
	out.text( code.text )
	out.endCode()
}

def codeblock( out, code )
{
	out.startCodeBlock()
	out.text( code.text )
	out.endCodeBlock()
}

def table( out, table )
{
	def title = table.findElement( "caption" )
	if( title )
	{
		out.startObjectHeader()
		dotitle( out, title )
		out.endObjectHeader()
	}
	out.startTable()
	for( child in table.children )
	{
		if( child.name == "tr" )
		{
			row( out, child )
		}
		else
			assert child.name == "caption" : "Got ${child.name}"
	}
	out.endTable()
}

def row( out, row )
{
	out.startRow()
	for( child in row.children )
	{
		if( child.name == "td" )
		{
			out.startCell()
			out.text( child.text )
			out.endCell()	
		}
		else if( child.name == "th" )
		{
			out.startHeaderCell()
			out.text( child.text )
			out.endHeaderCell()	
		}
		else
			assert false : "Got ${child.name}"
	}
	out.endRow()
}

def note( out, note )
{
	out.startNote()
	for( child in note.children )
	{
		if( child.name == "para" )
			para( out, child )
		else
			assert false : "Got ${child.name}"
	}
	out.endNote()
}
