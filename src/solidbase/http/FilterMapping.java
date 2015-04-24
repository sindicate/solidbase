package solidbase.http;

import java.util.regex.Pattern;

public class FilterMapping
{
	protected Pattern pattern;
	protected Filter filter;

	public FilterMapping( Pattern pattern, Filter filter )
	{
		this.pattern = pattern;
		this.filter = filter;
	}
}
