package solidstack.cbor;


public interface Token
{
	static public enum TYPE { UINT, NINT, BYTES, TEXT, ARRAY, MAP, IBYTES, ITEXT, IARRAY, IMAP, TAG, BOOL, NULL, UNDEF, HFLOAT, SFLOAT, DFLOAT, BREAK, EOF };

	static public final SimpleToken EOF = new SimpleToken( TYPE.EOF );
	static public final SimpleToken IBSTRING = new SimpleToken( TYPE.IBYTES );
	static public final SimpleToken ITSTRING = new SimpleToken( TYPE.ITEXT );
	static public final SimpleToken IARRAY = new SimpleToken( TYPE.IARRAY );
	static public final SimpleToken IMAP = new SimpleToken( TYPE.IMAP );
	static public final SimpleToken BREAK = new SimpleToken( TYPE.BREAK );
	static public final SimpleToken FALSE = new SimpleToken( false );
	static public final SimpleToken TRUE = new SimpleToken( true );
	static public final SimpleToken NULL = new SimpleToken( TYPE.NULL );
	static public final SimpleToken UNDEF = new SimpleToken( TYPE.UNDEF );

	TYPE type();
	boolean isTag();
	long longValue();
	double doubleValue();
	boolean booleanValue();
	int length();
	boolean hasTag( long value );
}
