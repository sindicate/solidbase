package solidstack.cbor;

public interface ByteStringIndex
{
	Integer putOrGet( ByteString value );
	int memoryUsage();
}
