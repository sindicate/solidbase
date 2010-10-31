package solidbase.http.hyperdb;

public class Table
{
	protected String name;
	protected int records;

	public Table( String name, int records )
	{
		this.name = name;
		this.records = records;
	}
}
