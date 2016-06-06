package solidstack.cbor;

import java.io.IOException;
import java.io.InputStream;

import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;


public class CBORBytesInputStream extends InputStream
{
	private CBORScanner in;
	private byte[] buffer;
	private int pos;


	public CBORBytesInputStream( CBORScanner in )
	{
		this.in = in;
	}

	// TODO Implement the other read

	@Override
	public int read() throws IOException
	{
		if( this.in == null )
			return -1;

		if( this.buffer != null && this.pos < this.buffer.length )
			return this.buffer[ this.pos++ ];

		Token t = this.in.get();
		if( t.getType() == TYPE.BREAK )
		{
			this.in = null;
			return -1;
		}

		if( t.getType() != TYPE.BSTRING && t.getType() != TYPE.TSTRING ) // TODO Add the type to constructor
			throw new IllegalStateException( "Only byte or text strings allowed, not " + t.getType() );

		this.buffer = new byte[ t.getLength() ];
		this.in.readBytes( this.buffer );
		this.pos = 0;
		return this.buffer[ this.pos++ ];
	}
}
