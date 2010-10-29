package solidbase.http;

import java.util.ArrayList;
import java.util.List;

public class Request
{
	protected String url;
	protected String parameters;
	protected List< Header > headers = new ArrayList< Header >();

	public void setUrl( String url )
	{
		this.url = url;
	}

	public String getUrl()
	{
		return this.url;
	}

	public void setParameters( String parameters )
	{
		this.parameters = parameters;
	}
}
