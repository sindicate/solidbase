package solidbase.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solidbase.util.Assert;

public class Request
{
	protected String url;
	protected String query;
	protected Map< String, List< String > > headers = new HashMap< String, List<String> >();
	protected Map< String, List< String > > parameters = new HashMap< String, List< String > >();
	protected String fragment;

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

	public void addHeader( String name, String value )
	{
		List<String> values = this.headers.get( name );
		if( values == null )
			this.headers.put( name, values = new ArrayList< String >() );
		values.add( value );
	}

	public String getParameter( String name )
	{
		List< String > values = this.parameters.get( name );
		if( values == null )
			return null;
		return values.get( 0 );
	}

	public String getHeader( String name )
	{
		List< String > values = this.headers.get( name );
		if( values == null )
			return null;
		Assert.isTrue( !values.isEmpty() );
		if( values.size() > 1 )
			throw new IllegalStateException( "Found more than 1 value for the header " + name );
		return values.get( 0 );
	}

	public boolean isConnectionClose()
	{
		return "close".equals( getHeader( "Connection" ) );
	}
}
