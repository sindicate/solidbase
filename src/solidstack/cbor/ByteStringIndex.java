package solidstack.cbor;

public interface ByteStringIndex
{
	Integer putOrGet( CBORByteString value );
	int memoryUsage();
}
