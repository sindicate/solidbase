package ronnie.dbpatcher;


public class Version
{
	protected String name;
	protected String description;
	protected boolean incomplete;
	
	public Version( String name, String description, boolean incomplete )
	{
		this.name = name;
		this.description = description;
		this.incomplete = incomplete;
	}

	public String getDescription()
	{
		return this.description;
	}

	public boolean isIncomplete()
	{
		return this.incomplete;
	}

	public String getName()
	{
		return this.name;
	}
}
