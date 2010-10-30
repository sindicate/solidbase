package solidbase.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request
{
	protected String url;
	protected String query;
	protected List< Header > headers = new ArrayList< Header >();
	protected Map< String, List< String > > parameters = new HashMap< String, List< String > >();

	public void setUrl( String url )
	{
		this.url = url;
	}

	public String getUrl()
	{
		return this.url;
	}

	public void setParameters( String query )
	{
		this.query = query;
	}

	public void addParameter( String name, String value )
	{
		List< String > values = this.parameters.get( name );
		if( values == null )
			this.parameters.put( name, values = new ArrayList< String >() );
		values.add( value );
	}

	public String getParameter( String name )
	{
		List< String > values = this.parameters.get( name );
		if( values == null )
			return null;
		return values.get( 0 );
	}
}
