package solidbase.http;

import java.util.ArrayList;
import java.util.List;

public class RequestHeader
{
	protected List< HeaderField > fields = new ArrayList< HeaderField >();

	public void addField( String field, String value )
	{
		this.fields.add( new HeaderField( field, value ) );
	}
}
