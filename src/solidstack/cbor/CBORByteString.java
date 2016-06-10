package solidstack.cbor;

import java.util.Arrays;


public class CBORByteString
{
	private boolean utf8;
	private byte[] bytes;


	public CBORByteString( boolean utf8, byte[] bytes )
	{
		this.utf8 = utf8;
		this.bytes = bytes;
	}

	@Override
	public int hashCode()
	{
		return 31 * Arrays.hashCode( this.bytes ) + ( this.utf8 ? 1 : 0 );
	}

	@Override
	public boolean equals( Object other )
	{
		if( other == this )
			return true;
		if( !( other instanceof CBORByteString ) )
			return false;
		CBORByteString that = (CBORByteString)other;
		if( that.utf8 != this.utf8 )
			return false;
		return Arrays.equals( that.bytes, this.bytes );
	}

	public int length()
	{
		return this.bytes.length;
	}

	public byte[] unwrap()
	{
		return this.bytes;
	}
}
