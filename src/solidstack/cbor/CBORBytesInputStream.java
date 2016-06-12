package solidstack.cbor;

import java.io.IOException;
import java.io.InputStream;

import solidstack.cbor.CBORToken.TYPE;


public class CBORBytesInputStream extends InputStream
{
	private CBORParser in;
	private byte[] buffer;
	private int pos;


	public CBORBytesInputStream( CBORParser in )
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

		CBORToken t = this.in.get();
		if( t.type() == TYPE.BREAK )
		{
			this.in = null;
			return -1;
		}

		if( t.type() != TYPE.BYTES && t.type() != TYPE.TEXT ) // TODO Add the type to constructor
			throw new IllegalStateException( "Only byte or text strings allowed, not " + t.type() );

		this.buffer = new byte[ t.length() ];
		this.in.readBytesForStream( this.buffer );
		this.pos = 0;
		return this.buffer[ this.pos++ ];
	}
}
