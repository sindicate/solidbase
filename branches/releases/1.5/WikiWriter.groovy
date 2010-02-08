import java.io.PrintWriter;

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

class WikiWriter
{
	PrintWriter out
	int sectiondepth = 0
	int chapternumber = 0
	boolean column1 = true
	boolean notext = true
	boolean header = false
	boolean code = false
	boolean codeblock = false
	boolean objectheader = false
	int itemizedlistdepth = 0
	boolean endedwithfreetext = false
	boolean firstcell = false
	boolean note = false
	int indent = 0
			
	def WikiWriter( PrintWriter printWriter )
	{
		out = printWriter
	}
	
	def startSection()
	{
		sectiondepth++
		if( sectiondepth == 1 )
			chapternumber++
	}
	
	def endSection()
	{
		sectiondepth--
	}
	
	def startPara()
	{
		if( itemizedlistdepth == 0 )
		{
			newline()
			if( endedwithfreetext )
				out.println()
		}
	}
	
	def endPara()
	{
		
	}
	
	def startHeader()
	{
		header = true
	}
	
	def endHeader()
	{
		header = false
	}
	
	def startCode()
	{
		code = true
	}
	
	def endCode()
	{
		code = false
	}
	
	def startCodeBlock()
	{
		codeblock = true
		if( itemizedlistdepth > 0 )
		{
			if( !notext )
				out.print( "<br/>" )
		}
		else
			newline()
	}
	
	def endCodeBlock()
	{
		codeblock = false
		if( itemizedlistdepth > 0 )
			out.print( "<br/>" )
		else
			newline()
	}
	
	def startItemizedList()
	{
		itemizedlistdepth++
	}
	
	def endItemizedList()
	{
		itemizedlistdepth--
	}
	
	def startItem()
	{
		newline()
		itemizedlistdepth.times { out.print( " " ) }
		out.print( "* " )
		column1 = false
	}
	
	def endItem()
	{
		newline()
	}
	
	def startObjectHeader()
	{
		objectheader = true
		newline()
	}
	
	def endObjectHeader()
	{
		objectheader = false
		newline()
	}
	
	def startTable()
	{
		
	}
	
	def endTable()
	{
		out.println() // Need empty line, otherwise strange positioning happens
	}
	
	def startRow()
	{
		firstcell = true
		newline()
	}
	
	def endRow()
	{
		newline()
	}
	
	def startCell()
	{
		if( firstcell )
		{
			out.print( "||" )
			firstcell = false
		}
		out.print( " " )
	}
	
	def endCell()
	{
		out.print( " ||" )
	}
	
	def startHeaderCell()
	{
		if( firstcell )
		{
			out.print( "||" )
			firstcell = false
		}
		out.print( " *" )
	}
	
	def endHeaderCell()
	{
		out.print( "* ||" )
	}
	
	def startNote()
	{
		note = true
		indent++
		startObjectHeader()
		text( "Note" )
		endObjectHeader()
	}
	
	def endNote()
	{
		note = false
		indent--
	}
	
	def text( text )
	{
		if( !text )
			return
			
		endedwithfreetext = false
		if( !code && !codeblock )
		{
			text = text.replaceAll( "[A-Z]+[a-z]+[A-Z]+[a-zA-Z]*", '!$0' )
			text = text.replaceAll( "[<>]", '`$0`' )
		}
		if( header )
		{
			newline()
			sectiondepth.times { out.print( "=" ) }
			if( sectiondepth == 1 )
				out.print( "Chapter ${chapternumber}. " )
			if( code )
				out.print( "{{{" )
			out.print( text )
			if( code )
				out.print( "}}}" )
			sectiondepth.times { out.print( "=" ) }
			column1 = false
			notext = false
		}
		else if( objectheader )
		{
			newline()
			( itemizedlistdepth + indent ).times { out.print( " " ) }
			out.print( "=====" )
			if( code )
				out.print( "{{{" )
			out.print( text )
			if( code )
				out.print( "}}}" )
			out.print( "=====" )
			column1 = false
			notext = false
		}
		else if( note )
		{
			if( column1 )
				text = text.replaceAll( '^\\s+', "" )
			( itemizedlistdepth + indent ).times { out.print( " " ) }
			out.print( text.replaceAll( "\\s{2,}", " " ) )
			column1 = false
			notext = false
			endedwithfreetext = true
		}
		else if( itemizedlistdepth > 0 )
		{
			if( notext )
				if( !code && !codeblock )
					text = text.replaceAll( '^\\s+', "" )
			if( code || codeblock )
				text = newlinetobr( text )
			else
				text = text.replaceAll( "\\s{2,}", " " )
			out.print( text )
			column1 = false
			notext = false
		}
		else if( codeblock )
		{
			out.println( "{{{" )
			out.println( text )
			out.println( "}}}" )
		}
		else if( code )
		{
			out.print( "{{{" + text + "}}}" )
		}
		else
		{
			if( column1 )
				text = text.replaceAll( '^\\s+', "" )
			out.print( text.replaceAll( "\\s{2,}", " " ) )
			column1 = false
			notext = false
			endedwithfreetext = true
		}
	}
	
	def newline()
	{
		if( !column1 )
		{
			out.println()
			column1 = true
			notext = true
		}
	}
	
	def newlinetobr( text )
	{
		return "{{{" + text.replaceAll( '\\n', "}}}<br/>{{{" ) + "}}}"
	}
	
	def todo( text )
	{
		text( "\"TODO: ${text}\"" )
	}
}
