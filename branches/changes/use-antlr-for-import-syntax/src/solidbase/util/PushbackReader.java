package solidbase.util;

import java.io.IOException;
import java.io.Reader;

import solidbase.core.Assert;

public class PushbackReader
{
	protected Reader reader;
	protected StringBuilder back;
	protected int backIndex;
	protected int lineNumber;

	public PushbackReader( Reader reader, int lineNumber )
	{
		this.reader = reader;
		this.back = new StringBuilder();
		this.backIndex = 0;
		this.lineNumber = lineNumber;
	}

	public int getLineNumber()
	{
		return this.lineNumber;
	}

	public Reader getReader()
	{
		Assert.isTrue( this.back.length() == 0 );
		return this.reader;
	}

	public int read()
	{
		if( this.back.length() > 0 )
		{
			int p = this.back.length() - 1;
			int ch = this.back.charAt( p );
			this.back.delete( p, p + 1 );
			if( ch == '\n' ) // There are no \r in the backbuffer
				this.lineNumber++;
			return ch;
		}
		try
		{
			int ch = this.reader.read();
			if( ch == '\r' )
			{
				ch = this.reader.read();
				if( ch != '\n' )
					push( ch );
				this.lineNumber++;
				return '\n';
			}
			else if( ch == '\n' )
				this.lineNumber++;
			return ch;
		}
		catch( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	public void push( int ch )
	{
		if( ch != -1 )
		{
			if( ch == '\n' )
				this.lineNumber--;
			this.back.append( (char)ch );
		}
	}

	public void push( StringBuilder buffer )
	{
		int len = buffer.length();
		while( len > 0 )
			push( buffer.charAt( --len ) ); // Use push to decrement the linenumber when a \n is found
	}

	public void push( String s )
	{
		int len = s.length();
		while( len > 0 )
			push( s.charAt( --len ) ); // Use push to decrement the linenumber when a \n is found
	}
}
