package solidbase.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import org.testng.Assert;

import solidbase.util.RandomAccessLineReader;

public class MockRandomAccessLineReader
{
	public String contents;
	public BufferedReader reader;
	public int currentLineNumber;
	public RandomAccessLineReader it;

	protected MockRandomAccessLineReader( String contents )
	{
		this.contents = contents;
		this.reader = new BufferedReader( new StringReader( this.contents ) );
		this.currentLineNumber = 1;
	}

	public void $init( URL url )
	{
	}

	public String readLine() throws IOException
	{
		String result = this.reader.readLine();
		if( result != null )
			this.currentLineNumber++;
		return result;
	}

	public int getLineNumber()
	{
		return this.currentLineNumber;
	}

	public void gotoLine( int lineNumber ) throws IOException
	{
		Assert.assertEquals( lineNumber, 1 );
		this.reader = new BufferedReader( new StringReader( this.contents ) );
		this.currentLineNumber = 1;
	}

//	@Mock
//	public void close() throws IOException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public void detectEncoding( String firstLine ) throws UnsupportedEncodingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public byte[] getBOM()
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public String getEncoding()
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public int getLineNumber()
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public void gotoLine( int lineNumber ) throws IOException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public void reOpen() throws UnsupportedEncodingException, IOException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Mock
//	public void reOpen( String encoding ) throws UnsupportedEncodingException, IOException
//	{
//		throw new NotImplementedException();
//	}
}
