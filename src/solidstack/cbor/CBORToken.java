package solidstack.cbor;


public interface CBORToken
{
	static public enum TYPE { UINT, NINT, BYTES, TEXT, ARRAY, MAP, IBYTES, ITEXT, IARRAY, IMAP, TAG, BOOL, NULL, UNDEF, HFLOAT, SFLOAT, DFLOAT, BREAK, EOF };

	static public final CBORSimpleToken EOF = new CBORSimpleToken( TYPE.EOF );
	static public final CBORSimpleToken IBSTRING = new CBORSimpleToken( TYPE.IBYTES );
	static public final CBORSimpleToken ITSTRING = new CBORSimpleToken( TYPE.ITEXT );
	static public final CBORSimpleToken IARRAY = new CBORSimpleToken( TYPE.IARRAY );
	static public final CBORSimpleToken IMAP = new CBORSimpleToken( TYPE.IMAP );
	static public final CBORSimpleToken BREAK = new CBORSimpleToken( TYPE.BREAK );
	static public final CBORSimpleToken FALSE = new CBORSimpleToken( false );
	static public final CBORSimpleToken TRUE = new CBORSimpleToken( true );
	static public final CBORSimpleToken NULL = new CBORSimpleToken( TYPE.NULL );
	static public final CBORSimpleToken UNDEF = new CBORSimpleToken( TYPE.UNDEF );

	TYPE type();
	boolean isTag();
	long longValue();
	double doubleValue();
	boolean booleanValue();
	int length();
	boolean hasTag( long value );
}
