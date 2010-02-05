/*--
 * Copyright 2006 René M. de Bloois
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

def book = new XmlSlurper().parse( "doc/manual.xml" as File )

new File( "../solidbase-wiki/UsersManual.wiki" ).withPrintWriter
{
	def info = book.info
	it.println "=${info.title}="
	it.println "==${info.subtitle}=="
	it.println "===${info.author.personname}==="
	it.println "----"
	
	def chapters = book.children()
	for( chapter in chapters )
	{
		if( chapter.name() == "chapter" )
		{
			it.println "=${chapter.title}="
			chapterorsection( it, chapter, 0 )
		}
		else
			assert chapter.name() == "info" : "Unexpected child of book: " + chapter.name()
	}
}

def chapterorsection( out, node, int sectiondepth )
{
	for( child in node.children() )
	{
		if( child.name() == "section" )
		{
			out.println "==${child.title}=="
			chapterorsection( out, child, sectiondepth + 1 )
		}
		else if( child.name() == "para" )
		{
			out.println "para"
		}
		else if( child.name() == "itemizedlist" )
		{
			out.println "itemizedlist"
					}
		else if( child.name() == "example" )
		{
			out.println "example"
		}
		else if( child.name() == "screen" )
		{
			out.println "screen"
		}
		else if( child.name() == "table" )
		{
			out.println "table"
		}
		else if( child.name() == "section" )
		{
			out.println "section"
		}
		else if( child.name() == "programlisting" )
		{
			out.println "programlisting"
		}
		else if( child.name() == "note" )
		{
			out.println "note"
		}
		else
			assert child.name() == "title" : "Unexpected child ${child.name()} of parent ${node.name()}"
	}
}
