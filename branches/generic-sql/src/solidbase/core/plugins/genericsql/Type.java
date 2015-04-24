package solidbase.core.plugins.genericsql;

public class Type
{
	private String type;
	private String length;

	public void setType( String type )
	{
		this.type = type;
	}

	public void setLength( String length )
	{
		this.length = length;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder( this.type );
		if( this.length != null )
		{
			result.append( '(' );
			result.append( this.length );
			result.append( ')' );
		}
		return result.toString();
	}
}
