package solidstack.cbor;


public class CBORTaggedToken extends CBORSimpleToken
{
	long[] tags;


	CBORTaggedToken( TYPE type )
	{
		super( type );
	}

	@Override
	public boolean hasTag( long value )
	{
		for( long tag : this.tags )
			if( tag == value )
				return true;
		return false;
	}
}
