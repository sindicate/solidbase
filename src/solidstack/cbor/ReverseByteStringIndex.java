package solidstack.cbor;

public interface ReverseByteStringIndex
{
	void put( CBORByteString value );
	CBORByteString get( int index );
}
