package solidstack.cbor;

public interface ReverseByteStringIndex
{
	void put( ByteString value );
	ByteString get( int index );
}
