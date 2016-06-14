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

	@Override
	public String toString()
	{
		if( this.tags == null )
			return super.toString();

		StringBuilder result = new StringBuilder( this.tags.length > 1 ? "TAGS" : "TAG" );
		for( long tag : this.tags )
		{
			result.append( " 0x" );
			appendHex( result, tag );
		}
		result.append( ' ' ).append( super.toString() );
		return result.toString();
	}
}
