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

try
{
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
		
		def out = new WikiWriter( it )
		
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
			out.todo( child.name )
		else if( child.name == "screen" )
			out.todo( child.name )
		else if( child.name == "table" )
			out.todo( child.name )
		else if( child.name == "programlisting" )
			out.todo( child.name )
		else if( child.name == "note" )
			out.todo( child.name )
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
		else if( child.name == "computeroutput" || child.name == "programlisting" )
			codeblock( out, child )
		else if( child.name == "xref" )
			out.todo( child.name )
		else if( child.name == "note" )
			out.todo( child.name )
		else if( child.name == "code" )
			out.todo( child.name )
		else
			assert false : "Got ${child.name}"
	}
	out.endPara()
}

def itemizedlist( out, section )
{
	out.startItemizedList()
	def title = section.findElement( "title" )
	if( title )
	{
		out.startObjectHeader()
		dotitle( out, title )
		out.endObjectHeader()
	}
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
