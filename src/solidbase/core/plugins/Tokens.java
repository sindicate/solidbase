package solidbase.core.plugins;

enum Tokens
{
	// Import
	SKIP,
	SEPARATED,
	ESCAPE,
	IGNORE,
	PREPEND,
	NOBATCH,
	INTO,
	EXEC,
	DATA,
	// Export
	WITH,
	DATE,
	COALESCE,
	COLUMN,
	FROM,
	BINARY,
	// Both
	LOG,
	FILE,
	EOF( null );

	private String name;

	private Tokens()
	{
		this.name = name();

	}
	private Tokens( String name )
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return this.name;
	}
}
