package solidstack.cbor;

import java.io.IOException;
import java.io.Reader;

import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;

public class CBORStringReader extends Reader
{
	private CBORScanner in;
	private String buffer;
	private int pos;

	public CBORStringReader( CBORScanner in )
	{
		this.in = in;
	}

	// TODO Implement the other read

	@Override
	public int read( char[] cbuf, int off, int len ) throws IOException
	{
		while( this.buffer == null || this.pos >= this.buffer.length() )
		{
			Token t = this.in.get();
			if( t.getType() != TYPE.TSTRING )
				throw new IllegalStateException( "Only text strings allowed" );

			this.buffer = this.in.readString( t.getLength() );
			this.pos = 0;
		}

		int end = this.buffer.length();
		if( end - this.pos > len )
			end = this.pos + len;
		this.buffer.getChars( this.pos, end, cbuf, off );
		int result = end - this.pos;
		this.pos = end;
		return result;
	}

	@Override
	public void close() throws IOException
	{
	}
}
